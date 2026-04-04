<?php
/**
 * Plugin Name: LibreCaptcha
 * Description: Integrates LibreCaptcha for user comments, login, and registration.
 * Version: 1.0.0
 * Author: LibreCaptcha
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit; // Exit if accessed directly
}

class LibreCaptchaPlugin {
    public function __construct() {
        add_action( 'admin_menu', array( $this, 'add_admin_menu' ) );
        add_action( 'admin_init', array( $this, 'register_settings' ) );

        // Add CAPTCHA to forms if enabled
        if ( get_option( 'lc_enable_login', 0 ) ) {
            add_action( 'login_form', array( $this, 'render_captcha' ) );
        }
        if ( get_option( 'lc_enable_registration', 0 ) ) {
            add_action( 'register_form', array( $this, 'render_captcha' ) );
        }
        if ( get_option( 'lc_enable_comments', 0 ) ) {
            add_action( 'comment_form_after_fields', array( $this, 'render_captcha' ) );
            add_action( 'comment_form_logged_in_after', array( $this, 'render_captcha' ) );
        }

        // Register shortcode
        add_shortcode( 'librecaptcha', array( $this, 'render_captcha_shortcode' ) );

        // AJAX actions for testing settings
        add_action( 'wp_ajax_lc_test_load', array( $this, 'ajax_test_load' ) );
        add_action( 'wp_ajax_lc_test_check', array( $this, 'ajax_test_check' ) );

        // Verification hooks
        if ( get_option( 'lc_enable_login', 0 ) ) {
            add_filter( 'authenticate', array( $this, 'verify_login_captcha' ), 20, 3 );
        }
        if ( get_option( 'lc_enable_registration', 0 ) ) {
            add_filter( 'registration_errors', array( $this, 'verify_registration_captcha' ), 10, 3 );
        }
        if ( get_option( 'lc_enable_comments', 0 ) ) {
            add_filter( 'pre_comment_on_post', array( $this, 'verify_comment_captcha' ) );
        }
    }

    private function verify_captcha() {
        $server_url = get_option( 'lc_server_url', '' );
        if ( empty( $server_url ) ) {
            return true; // Pass if not configured
        }

        if ( ! isset( $_POST['lc_captcha_id'] ) || ! isset( $_POST['lc_captcha_answer'] ) ) {
            return false;
        }

        $captcha_id = sanitize_text_field( $_POST['lc_captcha_id'] );
        $captcha_answer = sanitize_text_field( $_POST['lc_captcha_answer'] );

        $server_url = rtrim( $server_url, '/' );
        $auth_key = get_option( 'lc_auth_key', '' );

        $headers = array( 'Content-Type' => 'application/json' );
        if ( ! empty( $auth_key ) ) {
            $headers['Auth'] = $auth_key;
        }

        $response = wp_remote_post( $server_url . '/v2/answer', array(
            'headers'     => $headers,
            'body'        => wp_json_encode( array(
                'id'     => $captcha_id,
                'answer' => $captcha_answer,
            ) ),
            'method'      => 'POST',
            'data_format' => 'body',
        ) );

        if ( is_wp_error( $response ) ) {
            return false; // Fail safe or fail secure? typically fail secure for captcha
        }

        $body = wp_remote_retrieve_body( $response );
        $data = json_decode( $body, true );

        if ( isset( $data['result'] ) && ( $data['result'] === 'True' || $data['result'] === true ) ) {
            return true;
        }

        return false;
    }

    public function verify_login_captcha( $user, $username, $password ) {
        // Only check if it's a POST request (login attempt) and user is not already an error
        if ( $_SERVER['REQUEST_METHOD'] === 'POST' && ! is_wp_error( $user ) ) {
            if ( ! $this->verify_captcha() ) {
                return new WP_Error( 'authentication_failed', __( '<strong>ERROR</strong>: The CAPTCHA was incorrect.' ) );
            }
        }
        return $user;
    }

    public function verify_registration_captcha( $errors, $sanitized_user_login, $user_email ) {
        if ( $_SERVER['REQUEST_METHOD'] === 'POST' ) {
            if ( ! $this->verify_captcha() ) {
                $errors->add( 'captcha_failed', __( '<strong>ERROR</strong>: The CAPTCHA was incorrect.' ) );
            }
        }
        return $errors;
    }

    public function verify_comment_captcha( $comment_post_id ) {
        // If user is logged in, they might not see the captcha depending on form implementation,
        // but since we hooked `comment_form_logged_in_after`, they do see it.
        if ( $_SERVER['REQUEST_METHOD'] === 'POST' ) {
            if ( ! $this->verify_captcha() ) {
                wp_die( __( '<strong>ERROR</strong>: The CAPTCHA was incorrect. Please go back and try again.' ) );
            }
        }
    }

    public function ajax_test_load() {
        if ( ! current_user_can( 'manage_options' ) ) {
            wp_send_json_error( 'Unauthorized' );
        }

        $server_url = rtrim( get_option( 'lc_server_url', '' ), '/' );
        $auth_key = get_option( 'lc_auth_key', '' );
        $config_json_string = get_option( 'lc_config_json', '{"level":"easy","media":"image/png","input_type":"text","size":"350x100"}' );

        if ( empty( $server_url ) ) {
            wp_send_json_error( 'Server URL is not configured.' );
        }

        $headers = array( 'Content-Type' => 'application/json' );
        if ( ! empty( $auth_key ) ) {
            $headers['Auth'] = $auth_key;
        }

        $response = wp_remote_post( $server_url . '/v2/captcha', array(
            'headers'     => $headers,
            'body'        => $config_json_string,
            'method'      => 'POST',
            'data_format' => 'body',
        ) );

        if ( is_wp_error( $response ) ) {
            wp_send_json_error( 'Failed to connect to LibreCaptcha server.' );
        }

        $body = wp_remote_retrieve_body( $response );
        $data = json_decode( $body, true );

        if ( isset( $data['id'] ) ) {
            // Also return the full server URL so the client can load the image
            wp_send_json_success( array( 'id' => $data['id'], 'server_url' => $server_url ) );
        } else {
            wp_send_json_error( 'Invalid response from LibreCaptcha server.' );
        }
    }

    public function ajax_test_check() {
        if ( ! current_user_can( 'manage_options' ) ) {
            wp_send_json_error( 'Unauthorized' );
        }

        $captcha_id = sanitize_text_field( $_POST['captcha_id'] ?? '' );
        $captcha_answer = sanitize_text_field( $_POST['captcha_answer'] ?? '' );

        if ( empty( $captcha_id ) || empty( $captcha_answer ) ) {
            wp_send_json_error( 'Missing ID or Answer.' );
        }

        $server_url = rtrim( get_option( 'lc_server_url', '' ), '/' );
        $auth_key = get_option( 'lc_auth_key', '' );

        $headers = array( 'Content-Type' => 'application/json' );
        if ( ! empty( $auth_key ) ) {
            $headers['Auth'] = $auth_key;
        }

        $response = wp_remote_post( $server_url . '/v2/answer', array(
            'headers'     => $headers,
            'body'        => wp_json_encode( array(
                'id'     => $captcha_id,
                'answer' => $captcha_answer,
            ) ),
            'method'      => 'POST',
            'data_format' => 'body',
        ) );

        if ( is_wp_error( $response ) ) {
            wp_send_json_error( 'Failed to connect to LibreCaptcha server.' );
        }

        $body = wp_remote_retrieve_body( $response );
        $data = json_decode( $body, true );

        wp_send_json_success( $data );
    }

    public function render_captcha_shortcode() {
        ob_start();
        $this->render_captcha();
        return ob_get_clean();
    }

    public function render_captcha() {
        $server_url = get_option( 'lc_server_url', '' );
        if ( empty( $server_url ) ) {
            return;
        }

        $server_url = rtrim( $server_url, '/' );
        $auth_key = get_option( 'lc_auth_key', '' );
        $config_json_string = get_option( 'lc_config_json', '{"level":"easy","media":"image/png","input_type":"text","size":"350x100"}' );

        $headers = array( 'Content-Type' => 'application/json' );
        if ( ! empty( $auth_key ) ) {
            $headers['Auth'] = $auth_key;
        }

        $response = wp_remote_post( $server_url . '/v2/captcha', array(
            'headers'     => $headers,
            'body'        => $config_json_string,
            'method'      => 'POST',
            'data_format' => 'body',
        ) );

        if ( is_wp_error( $response ) ) {
            echo '<div class="librecaptcha-container" style="margin-bottom: 15px; color: red;">Error connecting to LibreCaptcha server.</div>';
            return;
        }

        $body = wp_remote_retrieve_body( $response );
        $data = json_decode( $body, true );

        if ( ! isset( $data['id'] ) ) {
            echo '<div class="librecaptcha-container" style="margin-bottom: 15px; color: red;">Error loading CAPTCHA. Invalid response.</div>';
            return;
        }

        $captcha_id = esc_attr( $data['id'] );
        $media_url = esc_url( $server_url . '/v2/media?id=' . $captcha_id );

        ?>
        <div class="librecaptcha-container" style="margin-bottom: 15px;">
            <div id="librecaptcha-image-container" style="margin-bottom: 10px;">
                <img src="<?php echo $media_url; ?>" alt="CAPTCHA" style="max-width: 100%;" />
            </div>
            <input type="hidden" name="lc_captcha_id" id="lc_captcha_id" value="<?php echo $captcha_id; ?>" />
            <input type="text" name="lc_captcha_answer" id="lc_captcha_answer" placeholder="Enter CAPTCHA here" required style="width: 100%; max-width: 350px;" />
        </div>
        <?php
    }

    public function add_admin_menu() {
        add_options_page(
            'LibreCaptcha Settings',
            'LibreCaptcha',
            'manage_options',
            'librecaptcha',
            array( $this, 'settings_page' )
        );
    }

    public function register_settings() {
        register_setting( 'librecaptcha_options_group', 'lc_server_url' );
        register_setting( 'librecaptcha_options_group', 'lc_auth_key' );
        register_setting( 'librecaptcha_options_group', 'lc_config_json' );
        register_setting( 'librecaptcha_options_group', 'lc_enable_login' );
        register_setting( 'librecaptcha_options_group', 'lc_enable_registration' );
        register_setting( 'librecaptcha_options_group', 'lc_enable_comments' );
    }

    public function settings_page() {
        ?>
        <div class="wrap">
            <h2>LibreCaptcha Settings</h2>
            <form method="post" action="options.php">
                <?php settings_fields( 'librecaptcha_options_group' ); ?>
                <?php do_settings_sections( 'librecaptcha_options_group' ); ?>
                <table class="form-table">
                    <tr valign="top">
                        <th scope="row">LibreCaptcha Server URL</th>
                        <td>
                            <input type="text" name="lc_server_url" value="<?php echo esc_attr( get_option('lc_server_url', '') ); ?>" class="regular-text" placeholder="http://localhost:8888" />
                            <p class="description">The URL to your LibreCaptcha instance (e.g. http://localhost:8888). Leave empty to disable.</p>
                        </td>
                    </tr>
                    <tr valign="top">
                        <th scope="row">Auth Key (Optional)</th>
                        <td>
                            <input type="text" name="lc_auth_key" value="<?php echo esc_attr( get_option('lc_auth_key', '') ); ?>" class="regular-text" placeholder="Secret Key" />
                            <p class="description">Optional auth key if your server requires it.</p>
                        </td>
                    </tr>
                    <tr valign="top">
                        <th scope="row">Config JSON</th>
                        <td>
                            <textarea name="lc_config_json" rows="5" cols="50" class="large-text code"><?php echo esc_textarea( get_option('lc_config_json', '{"level":"easy","media":"image/png","input_type":"text","size":"350x100"}') ); ?></textarea>
                            <p class="description">JSON configuration for the CAPTCHA requests.</p>
                        </td>
                    </tr>
                    <tr valign="top">
                        <th scope="row">Enable on Login Form</th>
                        <td>
                            <input type="checkbox" name="lc_enable_login" value="1" <?php checked( 1, get_option( 'lc_enable_login', 0 ), true ); ?> />
                        </td>
                    </tr>
                    <tr valign="top">
                        <th scope="row">Enable on Registration Form</th>
                        <td>
                            <input type="checkbox" name="lc_enable_registration" value="1" <?php checked( 1, get_option( 'lc_enable_registration', 0 ), true ); ?> />
                        </td>
                    </tr>
                    <tr valign="top">
                        <th scope="row">Enable on Comment Form</th>
                        <td>
                            <input type="checkbox" name="lc_enable_comments" value="1" <?php checked( 1, get_option( 'lc_enable_comments', 0 ), true ); ?> />
                        </td>
                    </tr>
                </table>
                <?php submit_button(); ?>
            </form>

            <hr>
            <h3>Test LibreCaptcha Connection</h3>
            <p>Use this section to verify your server configuration and ensure CAPTCHAs are loading and validating correctly.</p>
            <div id="lc-test-container" style="border: 1px solid #ccc; padding: 15px; max-width: 500px; background: #fff;">
                <button type="button" id="lc-test-load-btn" class="button button-secondary">Load Test CAPTCHA</button>
                <div id="lc-test-status" style="margin-top: 10px; font-weight: bold;"></div>

                <div id="lc-test-captcha-area" style="display: none; margin-top: 15px;">
                    <div id="lc-test-image-container" style="margin-bottom: 10px;"></div>
                    <input type="hidden" id="lc-test-captcha-id" value="" />
                    <input type="text" id="lc-test-captcha-answer" placeholder="Enter CAPTCHA answer" style="width: 100%; max-width: 350px; margin-bottom: 10px;" />
                    <br>
                    <button type="button" id="lc-test-check-btn" class="button button-primary">Check Answer</button>
                </div>
            </div>

            <script>
            document.addEventListener('DOMContentLoaded', function() {
                var loadBtn = document.getElementById('lc-test-load-btn');
                var checkBtn = document.getElementById('lc-test-check-btn');
                var statusEl = document.getElementById('lc-test-status');
                var captchaArea = document.getElementById('lc-test-captcha-area');
                var imageContainer = document.getElementById('lc-test-image-container');
                var idInput = document.getElementById('lc-test-captcha-id');
                var answerInput = document.getElementById('lc-test-captcha-answer');

                loadBtn.addEventListener('click', function() {
                    statusEl.innerText = 'Loading CAPTCHA...';
                    statusEl.style.color = '#0073aa';
                    captchaArea.style.display = 'none';
                    imageContainer.innerHTML = '';
                    answerInput.value = '';

                    var formData = new FormData();
                    formData.append('action', 'lc_test_load');

                    fetch(ajaxurl, {
                        method: 'POST',
                        body: formData
                    })
                    .then(function(response) {
                        return response.json();
                    })
                    .then(function(responseJson) {
                        if (responseJson.success && responseJson.data.id) {
                            var data = responseJson.data;
                            idInput.value = data.id;
                            var img = document.createElement('img');
                            img.src = data.server_url + '/v2/media?id=' + data.id;
                            img.alt = 'Test CAPTCHA';
                            img.style.maxWidth = '100%';
                            img.onload = function() {
                                statusEl.innerText = 'CAPTCHA loaded successfully.';
                                statusEl.style.color = 'green';
                                captchaArea.style.display = 'block';
                            };
                            img.onerror = function() {
                                statusEl.innerText = 'Error: Failed to load image from /v2/media';
                                statusEl.style.color = 'red';
                            };
                            imageContainer.appendChild(img);
                        } else {
                            throw new Error(responseJson.data || 'Invalid response format');
                        }
                    })
                    .catch(function(error) {
                        console.error('Test load error:', error);
                        statusEl.innerText = 'Error loading CAPTCHA: ' + error.message;
                        statusEl.style.color = 'red';
                    });
                });

                checkBtn.addEventListener('click', function() {
                    var captchaId = idInput.value;
                    var answer = answerInput.value;

                    if (!answer) {
                        alert('Please enter an answer.');
                        return;
                    }

                    statusEl.innerText = 'Checking answer...';
                    statusEl.style.color = '#0073aa';

                    var formData = new FormData();
                    formData.append('action', 'lc_test_check');
                    formData.append('captcha_id', captchaId);
                    formData.append('captcha_answer', answer);

                    fetch(ajaxurl, {
                        method: 'POST',
                        body: formData
                    })
                    .then(function(response) {
                        return response.json();
                    })
                    .then(function(responseJson) {
                        if (responseJson.success) {
                            var data = responseJson.data;
                            if (data && (data.result === 'True' || data.result === true)) {
                                statusEl.innerText = 'Success! Answer is correct.';
                                statusEl.style.color = 'green';
                            } else {
                                statusEl.innerText = 'Incorrect answer or expired.';
                                statusEl.style.color = 'red';
                            }
                        } else {
                            throw new Error(responseJson.data || 'Failed to verify');
                        }
                    })
                    .catch(function(error) {
                        console.error('Test check error:', error);
                        statusEl.innerText = 'Error checking answer: ' + error.message;
                        statusEl.style.color = 'red';
                    });
                });
            });
            </script>
        </div>
        <?php
    }
}

new LibreCaptchaPlugin();
