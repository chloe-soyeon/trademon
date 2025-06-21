let debounceTimer;
let suppressInputTrigger = false;
let chart = null;

$(document).ready(function () {


    const today = new Date();
    const oneMonthAgo = new Date();
    oneMonthAgo.setMonth(today.getMonth() - 1);
    const formatDate = (d) => d.toISOString().slice(0, 10);
    $("#startDate").val(formatDate(oneMonthAgo));
    $("#endDate").val(formatDate(today));

    $("#corpName").val("삼성전자");
    $("#selectedCorp").text("삼성전자");
    autoDisclosureSearch();

    $("#manualSearchBtn").on("click", function () {
        resetToBuyTab();
        autoDisclosureSearch();
    });

    $("#corpName").on("input", function () {
        if (suppressInputTrigger) {
            suppressInputTrigger = false;
            return;
        }

        clearTimeout(debounceTimer);
        const keyword = $(this).val();

        if (keyword.length > 1) {
            debounceTimer = setTimeout(() => {
                $.get("/stock/search", { keyword }, function (data) {
                    let suggestionList = "";
                    data.forEach(item => {
                        suggestionList += `<div onclick="selectCorp('${item.corpName}')">${item.corpName}</div>`;
                    });
                    $("#suggestions").html(suggestionList).show();
                });
            }, 300);
        } else {
            $("#suggestions").hide();
            $("#resultTable").hide();
            $("#no-result").hide();
            $("#priceInfoBox").hide();
        }
    });

    $(document).on("click", function (e) {
        if (!$(e.target).closest("#corpName").length && !$(e.target).closest("#suggestions").length) {
            $("#suggestions").hide();
        }
    });

    $("#corpName").on("keydown", function (e) {
        if (e.key === "Enter") {
            e.preventDefault();
            $("#suggestions").hide();
            resetToBuyTab();
            autoDisclosureSearch();
        }
    });

    $("#tradeButton").on("click", async function () {
        const tradeType = $("#tradeButton").text().includes("매수") ? "BUY" : "SELL";

        const dto = {
            assetName: $("#selectedCorp").text(),
            assetCode: stockCode,
            assetType: "STOCK",
            tradeType: tradeType,
            quntity: parseFloat($("#orderQty").val()),
            price: parseFloat($("#orderPrice").val().replace(/,/g, ""))
        };

        try {
            const res = await $.ajax({
                url: "/api/trade/execute",
                method: "POST",
                contentType: "application/json",
                data: JSON.stringify(dto)
            });

            if (res.status === "fail") {
                const swalOptions = {
                    icon: "error",
                    title: "거래 실패",
                    text: res.message || "요청이 정상적으로 처리되지 않았습니다."
                };

                if (res.redirectUrl) {
                    swalOptions.confirmButtonText = "로그인하러 가기";
                    Swal.fire(swalOptions).then(() => {
                        window.location.href = res.redirectUrl;
                    });
                } else {
                    swalOptions.confirmButtonText = "확인";
                    Swal.fire(swalOptions);
                }

                return;
            } else {
                Swal.fire({
                    icon: "success",
                    title: "거래 성공",
                    text: res.message || "정상적으로 거래가 처리되었습니다.",
                    confirmButtonText: "확인"
                }).then(async () => {
                    if ($("#sellTab").hasClass("active")) {
                        try {
                            const qtyRes = await $.get("/api/trade/availableQty", { assetCode: stockCode });
                            if (qtyRes.status === "success") {
                                $("#availableQty").text(qtyRes.availableQty);
                                $("#availableQtyBox").show();
                            } else {
                                $("#availableQtyBox").hide();
                            }
                        } catch (e) {
                            console.warn("❌ 매도 가능 수량 갱신 실패", e);
                            $("#availableQtyBox").hide();
                        }
                    }
                });
            }
        } catch (err) {
            console.error("❌ 거래 요청 중 오류 발생", err);
            Swal.fire({
                icon: "error",
                title: "서버 오류",
                text: "일시적인 오류가 발생했습니다. 다시 시도해주세요."
            });
        }
    });

    $(document).on("click", "#buyTab", function () {
        $("#buyTab").addClass("active");
        $("#sellTab").removeClass("active");
        $("#tradeButton").text("매수주문").css("background-color", "#f44336");
        $("#orderPriceText").text("매수 가격");
        $("#availableQtyBox").hide();
    });

    $(document).on("click", "#sellTab", async function () {
        $("#sellTab").addClass("active");
        $("#buyTab").removeClass("active");
        $("#tradeButton").text("매도주문").css("background-color", "#2196f3");
        $("#orderPriceText").text("매도 가격");

        try {
            const res = await $.get("/api/trade/availableQty", { assetCode: stockCode });
            if (res.status === "success") {
                $("#availableQty").text(res.availableQty);
                $("#availableQtyBox").show();
            } else {
                $("#availableQtyBox").hide();
            }
        } catch (err) {
            console.warn("❌ 매도 가능 수량 조회 실패", err);
            $("#availableQtyBox").hide();
        }
    });

    // lottie.loadAnimation({
    //     container: document.getElementById("lottieAnimation"),
    //     renderer: "svg",
    //     loop: true,
    //     autoplay: true,
    //     path: "https://lottie.host/7ad1be08-0732-4cd7-82eb-620160f0e384/9vIMFJ5kyV.json"
    // });

});

//구분
function resetToBuyTab() {
    $("#buyTab").addClass("active");
    $("#sellTab").removeClass("active");
    $("#tradeButton").text("매수주문").css("background-color", "#f44336");
    $("#orderPriceText").text("매수 가격");
    $("#availableQtyBox").hide();
}

function selectCorp(name) {
    suppressInputTrigger = true;
    $("#corpName").val(name);
    $("#selectedCorp").text(name);
    $("#suggestions").hide();
    resetToBuyTab();
    autoDisclosureSearch();
}

function getDisclosureList() {
    autoDisclosureSearch();
}

let stockCode = "";

async function autoDisclosureSearch() {
    const name = $("#corpName").val();
    $("#selectedCorp").text(name);
    const start = $("#startDate").val().replaceAll("-", "");
    const end = $("#endDate").val().replaceAll("-", "");

    $("#chartLoader").show(); // ✅ 로딩 애니메이션 표시
    $("#candleChart").html("");

    try {
        const disclosureList = await $.get("/stock/list", {
            corpName: name,
            startDate: start,
            endDate: end
        });

        if (disclosureList.length === 0) {
            $("#resultTable").hide();
            $("#no-result").show();
        } else {
            let rows = "";
            disclosureList.forEach(item => {
                rows += `
        <div class="col-md-6 col-lg-4 mb-3">
          <div class="card h-100 shadow-sm border-0">
            <div class="card-body">
              <h6 class="card-title fw-bold" style="color: #131b33;">📄 ${item.reportName}</h6>
              <p class="card-text text-muted small mb-2">📅 공시일: ${item.receiptDate}</p>
              <a href="${item.url}" target="_blank" class="btn btn-sm btn-outline-dark rounded-pill">공시 바로가기</a>
            </div>
          </div>
        </div>`;
            });

            $("#resultTableBody").html(rows);
            $("#resultTable").show();
            $("#no-result").hide();
        }
    } catch (err) {
        console.error("❌ 공시 목록 조회 실패:", err);
        $("#resultTable").hide();
        $("#no-result").show();
    }

    try {
        const krxResponse = await $.get("/stock/krx", { corpName: name });

        if (krxResponse.status === "fail") {
            Swal.fire({
                icon: "warning",
                title: "종목을 찾을 수 없습니다",
                html: `<p><strong>${name}</strong>에 대한 검색 결과가 없습니다.</p>`,
                confirmButtonText: "확인"
            });
            $("#priceInfoBox").hide();
            $("#candleChart").html("");
            $("#chartLoader").hide();
            return;
        }

        stockCode = krxResponse.stockCode;

        if (!stockCode) {
            $("#priceInfoBox").hide();
            $("#candleChart").html("");
            $("#chartLoader").hide();
            return;
        }

        const stockInfo = await $.get("/stock/stockinfo", { stockCode });

        $("#per").text(stockInfo.per ? stockInfo.per + "배" : "-");
        // $("#eps").text(stockInfo.eps ? stockInfo.eps + "원" : "-");
        $("#pbr").text(stockInfo.pbr ? stockInfo.pbr + "배" : "-");
        // $("#bps").text(stockInfo.bps ? stockInfo.bps + "원" : "-");
        $("#eps").text(stockInfo.eps ? Number(stockInfo.eps).toLocaleString() + "원" : "-");
        $("#bps").text(stockInfo.bps ? Number(stockInfo.bps).toLocaleString() + "원" : "-");


        $("#hts_frgn_ehrt").text(stockInfo.htsFrgnEhrt ? stockInfo.htsFrgnEhrt + "%" : "-");

        const formattedPrice = Number(stockInfo.currentPrice).toLocaleString();
        $("#currentPrice").text((formattedPrice || "-") + "원");
        $("#orderPrice").val(Number(stockInfo.currentPrice).toLocaleString() || "");

        $("#priceInfoBox").show();

        const candleData = await $.get("/stock/candlechart", { stockCode });

        if (!candleData || candleData.length === 0) {
            $("#candleChart").html("");
            $("#chartLoader").hide();
            return;
        }

        chart = LightweightCharts.createChart(document.getElementById("candleChart"), {
            width: document.getElementById("candleChart").offsetWidth,
            height: 400,
            layout: { backgroundColor: "#ffffff", textColor: "#000000" },
            grid: {
                vertLines: { color: "#e1e1e1" },
                horzLines: { color: "#e1e1e1" }
            },
            timeScale: { timeVisible: true, secondsVisible: false }
        });

        const candleSeries = chart.addCandlestickSeries({
            upColor: '#fb3d44',
            downColor: '#447ee6',
            borderVisible: false,
            wickUpColor: '#fb3d44',
            wickDownColor: '#447ee6'
        });

        candleSeries.setData(candleData.map(item => ({
            time: item.time,
            open: item.open,
            high: item.high,
            low: item.low,
            close: item.close
        })));

        chart.timeScale().fitContent();
        $("#chartLoader").hide();  // ✅ 차트 로딩 완료 후 애니메이션 숨기기

        window.addEventListener("resize", () => {
            if (chart) {
                chart.applyOptions({
                    width: document.getElementById("candleChart").offsetWidth,
                });
            }
        });

        const current = Number(stockInfo.currentPrice);
        const diff = Number(stockInfo.prdyVrss);
        const percent = parseFloat(stockInfo.prdyCtrt).toFixed(2);
        let sign = stockInfo.prdyVrssSign;

        if (sign === "4") sign = "3";
        if (sign === "5") sign = "2";

        let diffText = "";
        let color = "";

        if (sign === "1") {
            diffText = `▲${diff.toLocaleString()} (+${percent}%)`;
            color = "#d32f2f";
        } else if (sign === "2") {
            diffText = `▼${diff.toLocaleString()} (-${percent}%)`;
            color = "#1976d2";
        } else {
            diffText = `±0 (0.00%)`;
            color = "#666";
        }

        $("#priceChange").text(diffText).css("color", color);

    } catch (err) {
        console.error("❌ 투자지표 또는 차트 조회 실패:", err);
        $("#priceInfoBox").hide();
        $("#candleChart").html("");
        $("#chartLoader").hide();  // ✅ 오류 시 숨기기
    }
}

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
