package com.gomelev.app

import com.gomelev.app.data.PreloadedCatalog
import com.gomelev.app.domain.CreditCalculator
import com.gomelev.app.model.AppThemeMode
import com.gomelev.app.model.PdnVerdict
import com.gomelev.app.model.UserFinanceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CreditCalculatorTest {

    private val baseProfile = UserFinanceProfile(
        salaryByn = 2500.0,
        existingHousingCreditByn = 320.0,
        pdnRatioCap = 0.40,
        downPaymentPct = 0
    )

    @Test
    fun `user free pdn limit equals 680 byn`() {
        assertEquals(680.0, baseProfile.freePdnRoomByn, 0.001)
    }

    @Test
    fun `priorbank 84m max loan matches python reference`() {
        val prior = PreloadedCatalog.creditPrograms.first { it.id == "prior_bel_4" }
        val (maxLoan, conservative, _) = CreditCalculator.maxProgramLimits(prior, baseProfile)
        assertEquals(39287, maxLoan.roundToInt())
        assertEquals(37944, conservative.roundToInt())
    }

    @Test
    fun `dabrabyt annuity and belarusbank diff limits match python reference`() {
        val dabFix = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_fix" }
        val dabDiff = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_diff" }
        val belEx2 = PreloadedCatalog.creditPrograms.first { it.id == "belarus_ex2" }

        assertEquals(39212, CreditCalculator.maxProgramLimits(dabFix, baseProfile).first.roundToInt())
        assertEquals(30290, CreditCalculator.maxProgramLimits(dabDiff, baseProfile).first.roundToInt())
        assertEquals(33443, CreditCalculator.maxProgramLimits(belEx2, baseProfile).first.roundToInt())
    }

    @Test
    fun `xiaoma passes with 0 pct pv while geely ex2 requires pv 10 to 15 pct`() {
        val xiaoma = PreloadedCatalog.cars.first { it.id == "faw_xiaoma" }.copy(priceByn = 24990.0)
        val ex2 = PreloadedCatalog.cars.first { it.id == "geely_ex2_pro" }
        val dabFix = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_fix" }
        val prior = PreloadedCatalog.creditPrograms.first { it.id == "prior_bel_4" }
        val bgpb = PreloadedCatalog.creditPrograms.first { it.id == "bgpb_drive_24" }

        val xiaomaEval = CreditCalculator.evaluateCarWithProgram(xiaoma, dabFix, baseProfile)
        assertEquals(PdnVerdict.PASS, xiaomaEval.verdict)
        assertEquals(433, xiaomaEval.decidingMonthlyByn.roundToInt())

        // EX2 Pro at 0% PV fails Priorbank (777 > 680)
        val ex2Fail = CreditCalculator.evaluateCarWithProgram(ex2, prior, baseProfile)
        assertEquals(PdnVerdict.FAIL, ex2Fail.verdict)

        // EX2 Pro at 10% PV passes BGPB Drive (630 <= 680), borderline in Priorbank (699)
        val pv10Profile = baseProfile.copy(downPaymentPct = 10)
        assertEquals(PdnVerdict.PASS, CreditCalculator.evaluateCarWithProgram(ex2, bgpb, pv10Profile).verdict)
        assertEquals(PdnVerdict.BORDERLINE, CreditCalculator.evaluateCarWithProgram(ex2, prior, pv10Profile).verdict)

        // EX2 Pro at 15% PV passes Priorbank (661 <= 680)
        val pv15Profile = baseProfile.copy(downPaymentPct = 15)
        val ex2Prior15 = CreditCalculator.evaluateCarWithProgram(ex2, prior, pv15Profile)
        assertEquals(PdnVerdict.PASS, ex2Prior15.verdict)
        assertEquals(661, ex2Prior15.decidingMonthlyByn.roundToInt())
    }

    @Test
    fun `belarusbank EV defers principal but charges interest for six months`() {
        val car = PreloadedCatalog.cars.first { it.id == "faw_xiaoma" }.copy(priceByn = 24990.0)
        val program = PreloadedCatalog.creditPrograms.first { it.id == "belarus_ev_general" }
        val evaluation = CreditCalculator.evaluateCarWithProgram(car, program, UserFinanceProfile(downPaymentPct = 0))
        assertEquals(338.40625, evaluation.graceMonthlyByn, 0.001)
        assertEquals(568.02928, evaluation.peakPostGraceMonthlyByn, 0.01)
        assertEquals(PdnVerdict.PASS, evaluation.verdict)
        assertTrue(CreditCalculator.totalCreditPayments(evaluation) > car.priceByn)
    }

    @Test
    fun `savings deposit simulator grows above 10x monthly contribution`() {
        val saved = CreditCalculator.simulateDepositSavings(600.0, months = 10, annualRatePct = 12.0)
        assertTrue(saved > 6300.0 && saved < 6500.0)
    }

    @Test
    fun `default profile uses stated income and monthly obligations`() {
        val profile = UserFinanceProfile()
        assertEquals(10, profile.downPaymentPct)
        assertEquals(2400.0, profile.salaryByn, 0.001)
        assertEquals(320.0, profile.existingHousingCreditByn, 0.001)
        assertEquals(640.0, profile.freePdnRoomByn, 0.001)
        assertEquals(540.0, profile.copy(otherMonthlyDebtByn = 100.0).freePdnRoomByn, 0.001)
    }

    @Test
    fun `applicant restriction is distinct from payment and down payment fit`() {
        val car = PreloadedCatalog.cars.first { it.id == "faw_xiaoma" }.copy(priceByn = 24990.0)
        val online = PreloadedCatalog.creditPrograms.first { it.id == "belveb_online" }
        val branch = PreloadedCatalog.creditPrograms.first { it.id == "belarus_ev_general" }
        val profile = baseProfile.copy(mvdEmployee = true, deviceInstallmentByn = 120.0, downPaymentPct = 10)
        assertEquals(560.0, profile.freePdnRoomByn, 0.01)
        assertEquals(branch.id, CreditCalculator.bestEvaluationForCar(car, listOf(online, branch), profile)?.program?.id)
        assertEquals(online.id, CreditCalculator.bestEvaluationForCar(car, listOf(online, branch), profile.copy(mvdEmployee = false))?.program?.id)
        val higherAdvance = branch.copy(minDownPaymentPct = 25)
        assertEquals(null, CreditCalculator.bestEvaluationForCar(car, listOf(online, higherAdvance), profile))
        val ev = CreditCalculator.evaluateCarWithProgram(car, higherAdvance, profile)
        assertEquals(car.priceByn / 4, CreditCalculator.requiredDownPayment(ev), 0.01)
    }

    @Test
    fun `primary estimate uses published terms while cached alternative remains in bank list`() {
        val car = PreloadedCatalog.cars.first { it.id == "faw_xiaoma" }.copy(priceByn = 24990.0)
        val branch = PreloadedCatalog.creditPrograms.first { it.id == "belarus_ev_general" }
        val cheap = branch.copy(id = "example", postRatePct = 1.0, graceRatePct = 1.0, liveRateNote = null)
        val profile = baseProfile.copy(deviceInstallmentByn = 280.0)
        val programs = listOf(branch, cheap)
        assertEquals(branch.id, CreditCalculator.bestEvaluationForCar(car, programs, profile)?.program?.id)
        val alternative = CreditCalculator.evaluateAllProgramsForCar(car, programs, profile).first { it.program.id == cheap.id }
        assertEquals(PdnVerdict.PASS, alternative.verdict)
    }

    @Test
    fun `electric models have a sourced photograph`() {
        assertTrue(PreloadedCatalog.cars.filter { it.isEv }.all { it.imageRes != null && it.imageSource != null })
        assertEquals(3, AppThemeMode.entries.size)
    }
}
