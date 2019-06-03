document.getElementById("reg-btn").addEventListener("click", function(){
    var email = document.getElementById("email").value;
    var url = window.location.origin+"/v1/token?email="+email
    fetch(url)
    .then(res => res.json())
    .then((data) => {
        document.getElementById("token").innerHTML = "SECRET "+data.token;
    })
})
