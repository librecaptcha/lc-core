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

        $response = wp_remote_post( $server_url . '/v2/answer', array(
            'headers'     => array( 'Content-Type' => 'application/json' ),
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
        $config_json = get_option( 'lc_config_json', '{"level":"easy","media":"image/png","input_type":"text","size":"350x100"}' );

        ?>
        <div class="librecaptcha-container" style="margin-bottom: 15px;">
            <div id="librecaptcha-image-container" style="margin-bottom: 10px;">
                <!-- Image will be injected here -->
            </div>
            <input type="hidden" name="lc_captcha_id" id="lc_captcha_id" value="" />
            <input type="text" name="lc_captcha_answer" id="lc_captcha_answer" placeholder="Enter CAPTCHA here" required style="width: 100%; max-width: 350px;" />
        </div>
        <script>
        document.addEventListener('DOMContentLoaded', function() {
            var serverUrl = <?php echo json_encode( $server_url ); ?>;
            var configJson = <?php echo $config_json; ?>;

            fetch(serverUrl + '/v2/captcha', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(configJson)
            })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(function(data) {
                if (data && data.id) {
                    document.getElementById('lc_captcha_id').value = data.id;
                    var img = document.createElement('img');
                    img.src = serverUrl + '/v1/media?id=' + data.id;
                    img.alt = 'CAPTCHA';
                    img.style.maxWidth = '100%';
                    document.getElementById('librecaptcha-image-container').appendChild(img);
                }
            })
            .catch(function(error) {
                console.error('Error fetching LibreCaptcha:', error);
                document.getElementById('librecaptcha-image-container').innerText = 'Error loading CAPTCHA. Please refresh the page.';
            });
        });
        </script>
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
        </div>
        <?php
    }
}

new LibreCaptchaPlugin();
