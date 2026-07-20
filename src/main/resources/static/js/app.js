(function () {
    var captchaBtn = document.getElementById("captcha-refresh");
    var captchaImg = document.getElementById("captcha-img");
    if (captchaBtn && captchaImg) {
        captchaBtn.addEventListener("click", function () {
            captchaImg.src = (captchaImg.getAttribute("data-base") || "/captcha")
                + "?t=" + Date.now();
        });
    }

    document.querySelectorAll(".scope-item").forEach(function (item) {
        item.addEventListener("keydown", function (e) {
            if (e.key === "Enter" || e.key === " ") {
                var input = item.querySelector('input[type="checkbox"]');
                if (input) {
                    e.preventDefault();
                    input.checked = !input.checked;
                }
            }
        });
    });
})();
