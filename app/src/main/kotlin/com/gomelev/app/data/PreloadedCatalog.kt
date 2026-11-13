package com.gomelev.app.data

import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.CreditProgram
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.model.MacroDriver2027

object PreloadedCatalog {

    val dealers: List<DealerEntity> = preloadedDealersList

    val cars: List<CarOffer> = preloadedCarsList

    val creditPrograms: List<CreditProgram> = preloadedCreditsPart1List + preloadedCreditsPart2List

    val macroDrivers2027: List<MacroDriver2027> = listOf(
        MacroDriver2027(
            title = "Таможенная квота ЕЭК 20 000 EV исчерпана",
            tag = "Пошлина +15%",
            impactLabel = "Вверх на новый импорт",
            isPositiveForBuyer = false,
            detailRu = "Квота 2026 года выбрана ещё к сентябрю 2026. Без новой квоты на 2027 свежий ввоз облагается 15% пошлиной (+10–15% к ценнику)."
        ),
        MacroDriver2027(
            title = "Указ №428: 0% НДС при ввозе чистых EV",
            tag = "До 31.12.2028",
            impactLabel = "Защита от скачка +20%",
            isPositiveForBuyer = true,
            detailRu = "Нулевой НДС для чистых BEV (до 5 лет) продлён до конца 2028 года — это удерживает рынок от резкого обвала цен вверх, как у гибридов."
        ),
        MacroDriver2027(
            title = "Транспортный налог на электромобили",
            tag = "До 15.11.2027",
            impactLabel = "Первые начисления за 2026",
            isPositiveForBuyer = false,
            detailRu = "Освобождение отменено с 01.01.2026. Осенью 2027 владельцы EV впервые получат квитанции наравне с ДВС."
        ),
        MacroDriver2027(
            title = "Инфляция ~5,5% и прогноз курса ЕАБР",
            tag = "USD ~3,12–3,25 BYN",
            impactLabel = "Поддержка цен б/у vs старение",
            isPositiveForBuyer = true,
            detailRu = "На вторичке обычное старение (-12..-18% в год) борется с дорогим новым завозом — массовый BYD Seagull или Kia Niro EV могут слегка подешеветь на 5–8%."
        )
    )
}
