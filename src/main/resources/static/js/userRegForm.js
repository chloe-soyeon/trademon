// 이메일 인증번호
let emailAuthNumber = "";

$(document).ready(function () {
    const f = document.getElementById("f"); // form

    // 이메일 중복 확인
    $("#btnEmailCheck").on("click", function () {
        checkEmailDuplicateOnly(f);
    });

    // 이메일 인증번호 전송
    $("#btnEmailSend").on("click", function () {
        sendEmailAuth(f);
    });

    // 회원가입
    $("#btnSend").on("click", function () {
        doSubmit(f);
    });
});

// ✅ 이메일 중복 확인만 (인증번호 전송 안 함)
function checkEmailDuplicateOnly(f) {
    if (f.userEmail.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "이메일을 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userEmail.focus();
        return;
    }

    $.ajax({
        url: "/user/getEmailExists",
        type: "post",
        dataType: "JSON",
        data: $("#f").serialize(),
        success: function (json) {
            if (json.existsYn === "Y") {
                Swal.fire({
                    title: "잠깐만요!",
                    text: "이미 가입된 이메일이에요.",
                    icon: "error",
                    confirmButtonColor: '#273C6E'
                });
                f.userEmail.focus();
            } else {
                Swal.fire({
                    title: "사용 가능!",
                    text: "이 이메일은 사용하실 수 있어요 💌",
                    icon: "success",
                    confirmButtonColor: '#273C6E'
                });
            }
        },
        error: function () {
            Swal.fire({
                title: "문제가 생겼어요!",
                text: "이메일 확인 중 오류가 발생했어요.",
                icon: "error",
                confirmButtonColor: '#273C6E'
            });
        }
    });
}

// ✅ 인증번호 전송 (이메일 중복 확인은 X)
function sendEmailAuth(f) {
    if (f.userEmail.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "이메일을 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userEmail.focus();
        return;
    }

    $.ajax({
        url: "/user/send-auth",
        type: "post",
        dataType: "JSON",
        data: $("#f").serialize(),
        success: function (json) {
            if (json.existsYn === "Y") {
                Swal.fire({
                    title: "이미 가입됨",
                    text: "해당 이메일은 이미 사용 중이에요.",
                    icon: "error",
                    confirmButtonColor: '#273C6E'
                });
                f.userEmail.focus();
            } else if (json.authNumber) {
                emailAuthNumber = json.authNumber;
                Swal.fire({
                    title: "인증번호 전송 완료!",
                    text: "입력하신 이메일로 인증번호를 보냈어요 ✉️",
                    icon: "success",
                    confirmButtonColor: '#273C6E'
                });
            } else {
                Swal.fire({
                    title: "전송 실패",
                    text: "인증번호 발송에 실패했어요. 다시 시도해 주세요.",
                    icon: "error",
                    confirmButtonColor: '#273C6E'
                });
            }
        },
        error: function () {
            Swal.fire({
                title: "잠시만요!",
                text: "인증번호 요청 중 문제가 발생했어요.",
                icon: "error",
                confirmButtonColor: '#273C6E'
            });
        }
    });
}

// ✅ 회원가입 유효성 체크 및 제출
function doSubmit(f) {

    if (f.userEmail.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "이메일을 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userEmail.focus();
        return;
    }

    if (f.userName.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "이름을 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userName.focus();
        return;
    }

    if (f.userPwd.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "비밀번호를 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userPwd.focus();
        return;
    }

    if (f.userPwd2.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "비밀번호 확인도 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.userPwd2.focus();
        return;
    }

    if (f.userPwd.value !== f.userPwd2.value) {
        Swal.fire({
            title: "비밀번호 불일치!",
            text: "입력한 비밀번호가 서로 달라요.",
            icon: "error",
            confirmButtonColor: '#273C6E'
        });
        f.userPwd2.focus();
        return;
    }

    if (f.authNumber.value === "") {
        Swal.fire({
            title: "확인해주세요!",
            text: "인증번호를 입력해 주세요.",
            icon: "warning",
            confirmButtonColor: '#273C6E'
        });
        f.authNumber.focus();
        return;
    }

    if (f.authNumber.value != emailAuthNumber) {
        Swal.fire({
            title: "인증 실패!",
            text: "인증번호가 일치하지 않아요.",
            icon: "error",
            confirmButtonColor: '#273C6E'
        });
        f.authNumber.focus();
        return;
    }

    // Ajax로 회원가입 전송
    $.ajax({
        url: "/user/insertUserInfo",
        type: "post",
        dataType: "JSON",
        data: $("#f").serialize(),
        success: function (json) {
            if (json.result === 1) {
                Swal.fire({
                    title: "🎉 회원가입 완료!",
                    text: json.msg,
                    icon: "success",
                    confirmButtonColor: '#273C6E'
                }).then(() => {
                    location.href = "/user/login";
                });
            } else {
                Swal.fire({
                    title: "가입 실패",
                    text: json.msg,
                    icon: "error",
                    confirmButtonColor: '#273C6E'
                });
            }
        },
        error: function () {
            Swal.fire({
                title: "문제가 발생했어요",
                text: "회원가입 중 오류가 발생했어요. 다시 시도해 주세요.",
                icon: "error",
                confirmButtonColor: '#273C6E'
            });
        }
    });
}