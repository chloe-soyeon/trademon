

document.addEventListener("DOMContentLoaded", function() {
    console.log("DOMContentLoaded 이벤트 발생");
    loadTickerBar();
});

function loadTickerBar() {
    console.log("loadTickerBar 함수 호출됨");
    const track = $("#tickerTrack");
    track.empty();
    console.log("track 엘리먼트 내용 비움");

    $.get("/coin/prices", function (coinList) {
        console.log("/coin/prices API 응답:", coinList);
        const fragment = document.createDocumentFragment();
        const repeatCount = 2;

        for (let i = 0; i < coinList.length; i++) {
            const coin = coinList[i];
            const symbol = coin.symbol;
            const name = coin.name;
            const price = Number(coin.price).toLocaleString();

            const tickerItem = document.createElement("span");
            tickerItem.classList.add("ticker-item");
            tickerItem.innerHTML = `
                <img src="https://static.upbit.com/logos/${symbol}.png" alt="${symbol}" width="20" height="20" style="vertical-align:middle; margin-right:4px;">
                [${symbol}] ${name}: ${price}원
            `;
            fragment.appendChild(tickerItem);
        }

        // 반복 복사
        for (let i = 0; i < repeatCount; i++) {
            track[0].appendChild(fragment.cloneNode(true));
        }

        // 길이 측정 후 애니메이션 적용
        // requestAnimationFrame(() => {
        //     const width = track[0].scrollWidth;
        //     const speed = 60; // px/sec (너무 느리면 40~50으로 조절 가능)
        //     const duration = width / speed;
        //
        //     track.css({
        //         animation: `ticker ${duration}s linear infinite`
        //     });
        //
        //     console.log("🔁 애니메이션 적용됨: duration =", duration + "s");
        // });
        requestAnimationFrame(() => {
            const width = track[0].scrollWidth;
            const speed = 60; // px/sec
            const duration = width / speed;

            track[0].style.setProperty('--ticker-duration', `${duration}s`);
            track.addClass("ticker-animate");
        });


    }).fail(function (xhr, status, error) {
        console.error("❌ /coin/prices API 호출 실패:", error);
        $("#tickerTrack").html('<span class="ticker-item">시세 정보를 불러오는데 실패했습니다.</span>');
    });
}





$(document).ready(function () {
    let currentCoinCode = "KRW-BTC";
    let chart = null;
    let candleSeries = null;

    // function updatePrice(coinCode) {
    //     $.get("/coin/price", { coinCode: coinCode }, function (price) {
    //         $("#coinPrice").text(Number(price).toLocaleString());
    //     }).fail(function (xhr, status, error) {
    //         console.error("❌ 현재가 불러오기 실패:", error);
    //         $("#coinPrice").text("불러오기 실패");
    //     });
    // }

    // loadTickerBar();



    function loadChartData(coinCode) {
        $.get("/coin/candle", { coinCode: coinCode }, function (data) {
            console.log(`📦 [${coinCode}] 수신된 data:`, data);

            const chartData = data
                .map((item, i) => {
                    const time = Number(item.time);
                    const open = Number(item.open);
                    const high = Number(item.high);
                    const low = Number(item.low);
                    const close = Number(item.close);

                    if (!Number.isFinite(time) || !Number.isFinite(open) || !Number.isFinite(high) || !Number.isFinite(low) || !Number.isFinite(close)) {
                        console.warn(`❌ [${coinCode}] [${i}] 필수 필드 누락 또는 숫자 아님/무한대 → 제외`, item);
                        return null;
                    }
                    return { time, open, high, low, close };
                })
                .filter(Boolean)
                .sort((a, b) => a.time - b.time);

            console.log(`✅ [${coinCode}] 최종 변환 및 정렬된 chartData:`, chartData);

            if (chartData.length > 0) {
                if (candleSeries) {
                    candleSeries.setData(chartData);
                    chart.timeScale().fitContent();
                }
            } else {
                console.error(`⚠️ [${coinCode}] 차트에 표시할 데이터가 없습니다.`);
                if (candleSeries) {
                    candleSeries.setData([]);
                    chart.timeScale().reset();
                }
            }
            currentCoinCode = coinCode;
            updatePrice(currentCoinCode);
        }).fail(function (xhr, status, error) {
            console.error(`❌ [${coinCode}] 캔들 데이터 불러오기 실패:`, error);
            if (candleSeries) {
                candleSeries.setData([]);
                chart.timeScale().reset();
                $("#coinPrice").text("불러오기 실패");
            }
        });
    }

    function initializeChart(coinCode) {
        chart = LightweightCharts.createChart(document.getElementById("coinChart"), {
            width: document.getElementById("coinChart").offsetWidth,
            height: 400,
            layout: { background: { color: '#ffffff' }, textColor: '#000000' },
            grid: {
                vertLines: { color: '#f0f3fa' },
                horzLines: { color: '#f0f3fa' }
            },
            crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
            timeScale: { timeVisible: true, secondsVisible: false }
        });

        candleSeries = chart.addCandlestickSeries({
            upColor: '#fb3d44',
            downColor: '#447ee6',
            borderVisible: false,
            wickUpColor: '#fb3d44',
            wickDownColor: '#447ee6'
        });

        loadChartData(coinCode);
    }

    initializeChart(currentCoinCode);
    resetToBuyTab();

    function resetToBuyTab() {
        $("#buyTab").addClass("active");
        $("#sellTab").removeClass("active");
        $("#tradeButton").text("매수주문").css("background-color", "#f44336");
        $("#orderPriceText").text("매수 가격");
        $("#availableQtyBox").hide();
    }


    $("#searchInput").autocomplete({
        source: function (request, response) {
            $.get("/coin/api/crypto/search", { query: request.term }, function (data) {
                response($.map(data, function (item) {
                    return {
                        label: item.name + " (" + item.code + ")",
                        value: item.name,
                        code: item.code
                    };
                }));
            });
        },
        select: function (event, ui) {
            $("#searchInput").val(ui.item.value);
            updateCoinDisplay(ui.item.value, ui.item.code); // 🔥 이미지 + 이름 동시 갱신
            // $("#selectedCoin").text(ui.item.value);  // 🔁 이름 갱신 추가
            resetToBuyTab();                         // ✅ 매수 탭으로 초기화 추가
            loadChartData(ui.item.code);
            return false;
        }

    });

    // $("#searchButton").on("click", function () {
    //     const searchText = $("#searchInput").val().trim();
    //     $.get("/coin/api/crypto/search", { query: searchText }, function (data) {
    //         if (data.length > 0) {
    //             loadChartData(data[0].code); // 첫 번째 결과 사용 (정확도 높이기 위해 추가 로직 필요할 수 있음)
    //         } else {
    //             alert("해당하는 암호화폐를 찾을 수 없습니다.");
    //         }
    //     });
    //
    // });
    $("#searchInput").on("keypress", function (e) {
        if (e.which === 13) {
            const searchText = $(this).val().trim();
            if (searchText) {
                $.get("/coin/api/crypto/search", { query: searchText }, function (data) {
                    if (data.length > 0) {
                        $("#selectedCoin").text(data[0].name); // 🔁 이름 갱신
                        resetToBuyTab();                       // ✅ 매수 탭으로 초기화 추가
                        loadChartData(data[0].code);
                    } else {
                        alert("해당하는 암호화폐를 찾을 수 없습니다.");
                    }
                });
            }
        }
    });

    function updatePrice(coinCode) {
        $.get("/coin/price", { coinCode: coinCode }, function (price) {
            const formattedPrice = Number(price).toLocaleString();
            $("#coinPrice").text(formattedPrice);       // 상단 현재가 표시
            $("#orderPrice").val(formattedPrice);       // 📌 입력창에도 현재가 표시
        }).fail(function (xhr, status, error) {
            console.error("❌ 현재가 불러오기 실패:", error);
            $("#coinPrice").text("불러오기 실패");
            $("#orderPrice").val("불러오기 실패");
        });
    }

    $(document).on("click", "#buyTab", function () {
        $("#buyTab").addClass("active");
        $("#sellTab").removeClass("active");
        $("#tradeButton").text("매수주문").css("background-color", "#f44336");
        $("#orderPriceText").text("매수 가격");
        $("#availableQtyBox").hide();
    });

    $(document).on("click", "#sellTab", function () {
        $("#sellTab").addClass("active");
        $("#buyTab").removeClass("active");
        $("#tradeButton").text("매도주문").css("background-color", "#2196f3");
        $("#orderPriceText").text("매도 가격");

        // ✅ 여기 추가
        $.get("/api/trade/availableQty", { assetCode: currentCoinCode }, function (res) {
            if (res.status === "success") {
                $("#availableQty").text(res.availableQty);
                $("#availableQtyBox").show();
            } else {
                $("#availableQtyBox").hide();
            }
        });
    });






    window.addEventListener("resize", () => {
        if (chart) {
            chart.applyOptions({ width: document.getElementById("coinChart").offsetWidth });
        }
    });

    $("#tradeButton").on("click", async function () {


        const quantityStr = $("#orderQty").val().replace(/,/g, "");
        const quantity = parseFloat(quantityStr);

        // ✅ 수량이 0 이하 또는 숫자가 아닐 경우 거래 중단
        if (!Number.isFinite(quantity) || quantity <= 0) {
            Swal.fire({
                icon: "warning",
                title: "잘못된 수량",
                text: "0 이상의 수량을 입력해주세요."
            });
            return;
        }

        const tradeType = $("#tradeButton").text().includes("매수") ? "BUY" : "SELL";



        const dto = {
            assetType: "COIN",
            assetCode: currentCoinCode,
            assetName: $("#selectedCoin").text(),
            tradeType: tradeType,
            quntity: parseFloat($("#orderQty").val().replace(/,/g, "")),
            price: parseFloat($("#orderPrice").val().replace(/,/g, ""))
        };

        try {
            const res = await $.ajax({
                url: "/api/trade/execute",
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify(dto)
            });

            if (res.status === "success") {
                Swal.fire({
                    icon: "success",
                    title: "거래 완료",
                    text: res.message || "정상적으로 거래가 처리되었습니다."
                });

                if (tradeType === "SELL") {
                    const qtyRes = await $.get("/api/trade/availableQty", { assetCode: currentCoinCode });
                    if (qtyRes.status === "success") {
                        $("#availableQty").text(qtyRes.availableQty);
                        $("#availableQtyBox").show();
                    } else {
                        $("#availableQtyBox").hide();
                    }
                }
            } else {
                Swal.fire({
                    icon: "error",
                    title: "거래 실패",
                    text: res.message || "요청 처리 중 오류가 발생했습니다."
                });
            }

        } catch (err) {
            console.error("❌ 거래 요청 실패:", err);
            Swal.fire({
                icon: "error",
                title: "서버 오류",
                text: "잠시 후 다시 시도해주세요."
            });
        }
    });

});

$(document).on("click", ".btn-minus", function () {
    let qty = parseInt($("#orderQty").val()) || 1;
    if (qty > 1) {
        $("#orderQty").val(qty - 1);
    }
});

$(document).on("click", ".btn-plus", function () {
    let qty = parseInt($("#orderQty").val()) || 1;
    $("#orderQty").val(qty + 1);
});
function updateCoinDisplay(name, code) {
    // 한글 이름 출력
    $('#selectedCoin').text(name);

    // "KRW-BTC" → "BTC"
    const symbol = code.replace("KRW-", "");

    // 이미지 URL 설정
    const logoUrl = `https://static.upbit.com/logos/${symbol}.png`;
    $('#coinIcon')
        .attr('src', logoUrl)
        .attr('alt', symbol)
        .on('error', function () {
            // 이미지가 없을 경우 기본 이미지로 대체
            $(this).attr('src', '/images/default-coin.png');
        });
}

// function loadTickerBar() {
//     const track = $("#tickerTrack");
//     track.empty();
//
//     $.get("/coin/prices", function (coinList) {
//         coinList.forEach(coin => {
//             const symbol = coin.symbol;
//             const name = coin.name;
//             const price = Number(coin.price).toLocaleString();
//
//             const item = `
//                 <span class="ticker-item">
//                     <img src="https://static.upbit.com/logos/${symbol}.png" alt="${symbol}" width="20" height="20" style="vertical-align:middle; margin-right:4px;">
//                     [${symbol}] ${name}: ${price}원
//                 </span>
//             `;
//             track.append(item);
//         });
//
//         // 무한루프 자연스럽게
//         track.append(track.html());
//     });
// }







