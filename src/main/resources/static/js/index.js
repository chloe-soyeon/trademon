// 쿠키 설정
function setCookie(name, value, hours) {
    let d = new Date();
    d.setTime(d.getTime() + (hours * 60 * 60 * 1000));
    let expires = "expires=" + d.toUTCString();
    document.cookie = name + "=" + value + ";" + expires + ";path=/";
}

// 쿠키 가져오기
function getCookie(name) {
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    name = name + "=";
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i].trim();
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

// 팝업 표시 조건 확인
$(document).ready(function () {
    if (!getCookie("hideTodayPopup")) {
        $("#popupContainer").show();
    }

    $("#closePopup").click(function () {
        if ($("#hideToday").is(":checked")) {
            setCookie("hideTodayPopup", "true", 24); // 24시간 유지
        }
        $("#popupContainer").fadeOut();
    });
});