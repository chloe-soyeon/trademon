// HTML로딩이 완료되고, 실행됨
$(document).ready(function () {

    // 회원가입
    $("#btnUserReg").on("click", function () { // 버튼 클릭했을때, 발생되는 이벤트 생성함(onclick 이벤트와 동일함)
        location.href = "/user/userRegForm";
    })

    // 아이디 찾기
    $("#btnSearchUserId").on("click", function () { // 버튼 클릭했을때, 발생되는 이벤트 생성함(onclick 이벤트와 동일함)
        location.href = "/user/searchUserId";
    })

    // 비밀번호 찾기
    $("#btnSearchPassword").on("click", function () { // 버튼 클릭했을때, 발생되는 이벤트 생성함(onclick 이벤트와 동일함)
        location.href = "/user/searchPassword";
    })

    // 로그인
    $("#btnLogin").on("click", function () {
        let f = document.getElementById("f"); // form 태그

        if (f.userEmail.value === "") {
            Swal.fire({
                icon: 'warning',
                title: '확인해주세요!',
                text: '이메일을 입력해주세요.',
                confirmButtonColor: '#273C6E'
            });
            f.userEmail.focus();
            return;
        }

        if (f.userPwd.value === "") {
            Swal.fire({
                icon: 'warning',
                title: '확인해주세요!',
                text: '비밀번호를 입력해주세요!',
                confirmButtonColor: '#273C6E'
            });
            f.userPwd.focus();
            return;
        }

        $.ajax({
            url: "/user/loginProc",
            type: "post",
            dataType: "JSON",
            data: $("#f").serialize(),
            success: function (json) {
                if (json.result === 1) {
                    Swal.fire({
                        icon: 'success',
                        title: '🎉 로그인 성공!',
                        text: json.msg,
                        confirmButtonColor: '#273C6E'
                    }).then(() => {
                        location.href = "/user/index";
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: '로그인 실패',
                        text: json.msg,
                        confirmButtonColor: '#273C6E'
                    });
                    $("#userEmail").focus();
                }
            }
        });


    })
})