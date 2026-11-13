package com.gomelev.app

import com.gomelev.app.data.PreloadedCatalog
import com.gomelev.app.domain.CreditCalculator
import com.gomelev.app.model.PdnVerdict
import com.gomelev.app.model.UserFinanceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CreditCalculatorTest {

    private val sampleProfile = UserFinanceProfile(
        salaryByn = 2000.0,
        existingHousingCreditByn = 0.0,
        pdnRatioCap = 0.40,
        downPaymentPct = 0
    )

    @Test
    fun `default sample pdn limit equals 800 byn`() {
        assertEquals(800.0, sampleProfile.freePdnRoomByn, 0.001)
    }

    @Test
    fun `pdn subtracts existing obligation cleanly`() {
        val burdened = sampleProfile.copy(salaryByn = 3000.0, existingHousingCreditByn = 200.0)
        assertEquals(1000.0, burdened.freePdnRoomByn, 0.001)
    }

    @Test
    fun `priorbank 84m max loan scales proportionally with pdn`() {
        val prior = PreloadedCatalog.creditPrograms.first { it.id == "prior_bel_4" }
        val (maxLoan, conservative, _) = CreditCalculator.maxProgramLimits(prior, sampleProfile)
        assertTrue(maxLoan > 45000.0 && conservative < maxLoan)
    }

    @Test
    fun `dabrabyt annuity vs belarusbank diff limits remain ordered`() {
        val dabFix = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_fix" }
        val dabDiff = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_diff" }
        val belEx2 = PreloadedCatalog.creditPrograms.first { it.id == "belarus_ex2" }

        val fixLimit = CreditCalculator.maxProgramLimits(dabFix, sampleProfile).first
        val diffLimit = CreditCalculator.maxProgramLimits(dabDiff, sampleProfile).first
        val belLimit = CreditCalculator.maxProgramLimits(belEx2, sampleProfile).first
        assertTrue(fixLimit > belLimit && belLimit > diffLimit)
    }

    @Test
    fun `xiaoma passes with 0 pct pv while geely ex2 requires higher pv or budget`() {
        val xiaoma = PreloadedCatalog.cars.first { it.id == "faw_xiaoma" }
        val ex2 = PreloadedCatalog.cars.first { it.id == "geely_ex2_pro" }
        val dabFix = PreloadedCatalog.creditPrograms.first { it.id == "dabrabyt_avtonado_fix" }
        val prior = PreloadedCatalog.creditPrograms.first { it.id == "prior_bel_4" }

        val xiaomaEval = CreditCalculator.evaluateCarWithProgram(xiaoma, dabFix, sampleProfile)
        assertEquals(PdnVerdict.PASS, xiaomaEval.verdict)

        val pv0Profile = sampleProfile.copy(salaryByn = 1700.0, existingHousingCreditByn = 0.0)
        assertEquals(PdnVerdict.FAIL, CreditCalculator.evaluateCarWithProgram(ex2, prior, pv0Profile).verdict)

        val pv20Profile = pv0Profile.copy(downPaymentPct = 20)
        assertEquals(PdnVerdict.PASS, CreditCalculator.evaluateCarWithProgram(ex2, prior, pv20Profile).verdict)
    }

    @Test
    fun `savings deposit simulator grows above 10x monthly contribution`() {
        val saved = CreditCalculator.simulateDepositSavings(600.0, months = 10, annualRatePct = 12.0)
        assertTrue(saved > 6300.0 && saved < 6500.0)
    }
}
