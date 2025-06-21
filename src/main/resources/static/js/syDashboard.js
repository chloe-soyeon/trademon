$(document).ready(function () {
    loadSummary();
    drawProfitBarChart();
    drawTradePie();
    loadTradeHistory('ALL');


    $("#resetBtn").click(function () {
        if (!confirm("정말 자산을 초기화하시겠습니까?\n모든 거래 기록이 삭제되고 5,000만 원으로 다시 시작됩니다.")) return;

        $.post("/api/trade/reset", function (res) {
            if (res.status === "success") {
                alert("초기화 완료! 다시 5천만 원으로 시작합니다.");
                location.reload();
            } else {
                alert("초기화 실패: " + res.message);
            }
        }).fail(function () {
            alert("서버 오류로 초기화에 실패했습니다.");
        });
    });




    function loadSummary() {
        $.get("/api/trade/summary", function (res) {

            if (res.status === "fail" && res.message === "로그인이 필요합니다.") {
                // 아무 것도 하지 않음 - 이미 서버 렌더링된 HTML에서 로그인 안내 UI가 노출되고 있으므로
                $("#loadingBox").hide();       // 로딩 애니 제거
                $("#assetContent").hide();     // 자산 영역 숨김
                $(".table-responsive").hide();
                // $("#tradeToolbar").hide();
                // $(".trade-filter-group").closest(".d-flex").hide();
                return;
            }

            $("#loadingBox").hide(); // ✅ 성공 시 숨김
            $("#assetContent").fadeIn(200, function () {
                // ✅ 차트 강제 리사이즈 (fadeIn 완료 후)
                echarts.getInstanceByDom(document.getElementById('tradePie'))?.resize();
                echarts.getInstanceByDom(document.getElementById('assetChart'))?.resize();
            });

            if (res.status === "success") {
                const s = res.summary;
                $("#totalAsset").text(formatWon(s.totalAsset));
                $("#availableCash").text(formatWon(s.availableCash));
                $("#totalEval").text(formatWon(s.totalEval));
                $("#profit").text(formatWon(s.unrealizedProfit));
                $("#realizedProfit").text(formatWon(s.realizedProfit || 0));
                $("#rate").text(s.rate + "%");
                $("#orderAvailable").text(formatWon(s.availableCash));

                const profitVal = parseFloat(s.unrealizedProfit);
                const rateVal = parseFloat(s.rate);

                $("#profit").removeClass("text-danger text-primary")
                    .addClass(profitVal >= 0 ? "text-danger" : "text-primary");

                $("#rate").removeClass("text-danger text-primary")
                    .addClass(rateVal >= 0 ? "text-danger" : "text-primary");

                updateStatusImage();

            } else {
                alert(res.message || "요약 정보 조회 실패");
            }



        }).fail(function () {
            $("#loadingBox").hide();
            alert("서버 오류로 자산 정보를 불러올 수 없습니다.");
        });
    }

    function drawProfitBarChart() {
        const chart = echarts.init(document.getElementById('assetChart'));

        $.get("/api/trade/profit-ratio", function (res) {
            if (res.status === "success") {
                const data = res.data;
                const categories = data.map(item => item.name);
                const values = data.map(item => item.value);

                chart.setOption({
                    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
                    // title: {
                    //   text: '종목별 수익률 분석',
                    //   left: 'center',
                    //   textStyle: { color: '#2f88ff', fontSize: 16 }
                    // },
                    grid: {top: 60, bottom: 20, left: 20, right: 20, containLabel: true},
                    xAxis: {
                        type: 'value',
                        position: 'top',
                        splitLine: {lineStyle: {type: 'dashed', color: '#c0dfff'}},
                        axisLine: {lineStyle: {color: '#c0dfff'}}
                    },
                    yAxis: {
                        type: 'category',
                        data: categories,
                        axisTick: {show: false},
                        axisLine: {show: false},
                        axisLabel: {fontWeight: 'bold'}
                    },
                    series: [{
                        name: '수익률',
                        type: 'bar',
                        barWidth: 14,
                        label: {
                            show: false,
                            position: 'insideRight',
                            formatter: '{b}',
                            fontWeight: 'bold'
                        },
                        itemStyle: {
                            borderRadius: 4,
                            color: function (params) {
                                const v = params.value;
                                return v >= 0 ? new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                                    {offset: 0, color: '#ff4d4d'},
                                    {offset: 1, color: '#ffd3d3'}
                                ]) : new echarts.graphic.LinearGradient(1, 0, 0, 0, [
                                    {offset: 0, color: '#8bcafe'},
                                    {offset: 1, color: '#2f88ff'}
                                ]);
                            }
                        },
                        data: values
                    }]
                });
            } else {
                chart.setOption({title: {text: '데이터 없음'}});
            }
        });
    }

    function drawTradePie() {
        const pie = echarts.init(document.getElementById('tradePie'));

        $.get("/api/trade/holding-ratio", function (res) {
            if (res.status === "success") {
                pie.setOption({
                    color: [
                        '#1E2F5B',  // 삼성 딥블루
                        '#2C3E80',  // 네이비블루
                        '#4B6EA9',  // 스카이블루
                        '#8BAEDC',  // 페일블루
                        '#D0DEEF',  // 아이스블루
                        '#F0F4FA',  // 거의 흰 회색
                        '#8697A8',  // 블루그레이
                        '#5C6770',  // 차콜그레이
                        '#B8C4CE',  // 라이트그레이
                        '#3B3B3B',  // 블랙톤 중립 회색
                        '#728BAA',  // 실버블루
                        '#6C94B7',  // 소프트블루
                        '#BAC8D3',  // 은은한 청회색
                        '#7CA1BF',  // 워터블루
                        '#A0B4C8'   // 푸른기운의 연그레이
                    ]
                    ,
                    tooltip: {trigger: 'item'},
                    legend: {top: '5%', left: 'center'},
                    graphic: {
                        elements: [{
                            type: 'text', left: 'center', top: 'center',
                            style: {text: '', textAlign: 'center', fontSize: 28, fontWeight: 'bold', fill: '#333'}
                        }]
                    },
                    series: [{
                        name: '보유 금액',
                        type: 'pie',
                        radius: ['40%', '70%'],
                        minAngle: 5, // 👈 추가
                        avoidLabelOverlap: false,
                        itemStyle: {
                            borderRadius: 10,
                            borderColor: '#fff',
                            borderWidth: 2
                        },
                        label: {show: false},
                        labelLine: {show: false},
                        data: res.holdings
                    }]

                });

                pie.on('mouseover', function (params) {
                    if (params.seriesType === 'pie') {
                        pie.setOption({
                            graphic: {
                                elements: [{
                                    type: 'text',
                                    left: 'center', top: 'center',
                                    style: {
                                        text: params.name,
                                        textAlign: 'center',
                                        fontSize: 28,
                                        fontWeight: 'bold',
                                        fill: '#333'
                                    }
                                }]
                            }
                        });
                    }
                });

                pie.on('mouseout', function () {
                    pie.setOption({
                        graphic: {
                            elements: [{
                                type: 'text',
                                left: 'center', top: 'center',
                                style: {
                                    text: '',
                                    textAlign: 'center',
                                    fontSize: 28,
                                    fontWeight: 'bold',
                                    fill: '#333'
                                }
                            }]
                        }
                    });
                });

            } else {
                pie.setOption({title: {text: "데이터 없음"}});
            }
        });
    }

    function formatWon(value) {
        if (!value) return "₩0";
        return "₩" + Number(value).toLocaleString();
    }


});

window.addEventListener('resize', function () {
    echarts.getInstanceByDom(document.getElementById('tradePie'))?.resize();
    echarts.getInstanceByDom(document.getElementById('assetChart'))?.resize();
});

window.loadTradeHistory = function (filter) {
    const url = filter === 'ALL' ? "/api/trade/history" : `/api/trade/history?type=${filter}`;
    $.get(url, function (res) {
        console.log("📦 받은 응답:", res);

        const $table = $("#historyTable");
        $table.empty();

        if (res.status === "success") {
            const data = res.history;

            if (data.length === 0) {
                $table.append(`<div class="divTableRow"><div class="divTableCell" colspan="6">거래 내역이 없습니다.</div></div>`);
                return;
            }

            data.forEach(item => {
                const total = (parseFloat(item.quntity) * parseFloat(item.price)).toLocaleString();
                const typeText = item.tradeType === "SELL" ? "매도" : "매수";
                $table.append(`
                    <div class="divTableRow">
                        <div class="divTableCell text-center">${item.tradeTime}</div>
                        <div class="divTableCell text-center">${item.assetName}</div>
                        <div class="divTableCell text-center">${typeText}</div>
                        <div class="divTableCell text-center">${parseFloat(item.quntity).toLocaleString()}</div>
                        <div class="divTableCell text-center">${parseFloat(item.price).toLocaleString()}</div>
                        <div class="divTableCell text-center">${total}</div>
                    </div>
                `);
            });
        } else {
            $table.append(`<div class="divTableRow"><div class="divTableCell" colspan="6">데이터 불러오기 실패</div></div>`);
        }
    });

    $('#resetTradeBtn').on('click', function () {
        Swal.fire({
            title: '거래 내역을 초기화할까요?',
            text: '삭제된 거래는 복구할 수 없습니다.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: '초기화',
            cancelButtonText: '취소',
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6'
        }).then((result) => {
            if (result.isConfirmed) {
                $.post("/api/trade/reset", function (res) {
                    if (res.status === "success") {
                        Swal.fire({
                            title: '✅ 초기화 완료!',
                            text: '거래 내역이 초기화되었습니다.',
                            icon: 'success',
                            timer: 2000,
                            showConfirmButton: false
                        });
                        loadTradeHistoryWithActive(null, 'ALL'); // 거래내역 다시 로딩
                    } else {
                        Swal.fire('실패', res.message || '초기화 실패', 'error');
                    }
                }).fail(function () {
                    Swal.fire('서버 오류', '초기화 요청 중 문제가 발생했습니다.', 'error');
                });
            }
        });
    });



}


window.loadTradeHistoryWithActive = function (btn, filter) {
    $(".trade-filter-group button").removeClass("active");
    $(btn).addClass("active");
    loadTradeHistory(filter);
};

function updateStatusImage() {
    const rateText = document.getElementById('rate').innerText.replace('%', '').trim();
    const rateValue = parseFloat(rateText);

    const statusImage = document.getElementById('statusImage');
    const statusMessage = document.getElementById('statusMessage');

    if (!isNaN(rateValue)) {
        if (rateValue > 0) {
            statusImage.src = '/images/happy.png';
            statusMessage.innerText = '나 개미 아니고 상어임🦈';
        } else if (rateValue < 0) {
            statusImage.src = '/images/sad.png';
            statusMessage.innerText = '흑흑... 내 돈 어디 갔지 😢';
        } else {
            statusImage.src = '/images/melong.png';
            statusMessage.innerText = '놀고 있었지롱 😛';
        }
    }
}


// 자산 로딩 후 이미지도 업데이트
document.addEventListener("DOMContentLoaded", function () {
    setTimeout(updateStatusImage, 1000); // 데이터가 다 세팅된 이후 실행
});