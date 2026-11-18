package com.gomelev.app.domain

import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.CreditProgram
import com.gomelev.app.model.CreditScope
import com.gomelev.app.model.ForecastScenario
import com.gomelev.app.model.PaymentEvaluation
import com.gomelev.app.model.PdnVerdict
import com.gomelev.app.model.ScheduleType
import com.gomelev.app.model.UserFinanceProfile
import java.time.Year
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object CreditCalculator {

    fun annuityPayment(principal: Double, annualRatePct: Double, months: Int): Double {
        if (principal <= 0.0 || months <= 0) return 0.0
        val r = annualRatePct / 100.0 / 12.0
        if (r <= 0.0) return principal / months
        return principal * r / (1.0 - (1.0 + r).pow(-months))
    }

    fun annuityMaxPrincipal(maxPayment: Double, annualRatePct: Double, months: Int): Double {
        if (maxPayment <= 0.0 || months <= 0) return 0.0
        val r = annualRatePct / 100.0 / 12.0
        if (r <= 0.0) return maxPayment * months
        return maxPayment * (1.0 - (1.0 + r).pow(-months)) / r
    }

    data class SteppedAnnuityResult(
        val graceMonthly: Double,
        val postGraceMonthly: Double,
        val remainingPrincipalAfterGrace: Double,
        val fullRateFromP0Monthly: Double
    )

    fun steppedAnnuity(
        principal: Double,
        graceRatePct: Double,
        graceMonths: Int,
        postRatePct: Double,
        totalMonths: Int
    ): SteppedAnnuityResult {
        if (principal <= 0.0 || totalMonths <= 0) {
            return SteppedAnnuityResult(0.0, 0.0, 0.0, 0.0)
        }
        if (graceMonths <= 0 || graceMonths >= totalMonths) {
            val pmt = annuityPayment(principal, postRatePct, totalMonths)
            return SteppedAnnuityResult(pmt, pmt, principal, pmt)
        }
        val pmtGrace = annuityPayment(principal, graceRatePct, totalMonths)
        val ig = graceRatePct / 100.0 / 12.0
        var rem = principal
        repeat(graceMonths) {
            val interest = rem * ig
            rem -= (pmtGrace - interest)
        }
        rem = rem.coerceAtLeast(0.0)
        val pmtPost = annuityPayment(rem, postRatePct, totalMonths - graceMonths)
        val pmtP0Post = annuityPayment(principal, postRatePct, totalMonths)
        return SteppedAnnuityResult(pmtGrace, pmtPost, rem, pmtP0Post)
    }

    data class MaxSteppedAnnuityResult(
        val maxPostGracePrincipal: Double,
        val conservativeP0Principal: Double
    )

    fun maxSteppedAnnuityPrincipal(
        maxMonthlyPmt: Double,
        graceRatePct: Double,
        graceMonths: Int,
        postRatePct: Double,
        totalMonths: Int
    ): MaxSteppedAnnuityResult {
        val conservative = annuityMaxPrincipal(maxMonthlyPmt, postRatePct, totalMonths)
        val probe = steppedAnnuity(10_000.0, graceRatePct, graceMonths, postRatePct, totalMonths)
        val peakPer10k = max(probe.postGraceMonthly, probe.graceMonthly)
        val maxStepped = if (peakPer10k > 0.0) 10_000.0 * (maxMonthlyPmt / peakPer10k) else 0.0
        return MaxSteppedAnnuityResult(maxStepped, conservative)
    }

    data class DiffScheduleResult(
        val month1Grace: Double,
        val peakPostGrace: Double,
        val month1FullPostRate: Double,
        val lastMonth: Double
    )

    fun differentiatedDetails(
        principal: Double,
        graceRatePct: Double,
        graceMonths: Int,
        postRatePct: Double,
        totalMonths: Int,
        principalGraceMonths: Int = 0
    ): DiffScheduleResult {
        if (principal <= 0.0 || totalMonths <= 0) {
            return DiffScheduleResult(0.0, 0.0, 0.0, 0.0)
        }
        val deferred = principalGraceMonths.coerceIn(0, totalMonths - 1)
        val grace = graceMonths.coerceIn(0, totalMonths)
        val principalPayment = principal / (totalMonths - deferred)
        fun payment(month: Int, rate: Double): Double {
            val opening = principal - principalPayment * (month - deferred - 1).coerceAtLeast(0)
            return opening * rate / 1200 + if (month > deferred) principalPayment else 0.0
        }
        val peakDuring = when {
            grace == 0 -> payment(1, postRatePct)
            deferred < grace -> max(payment(1, graceRatePct), payment(deferred + 1, graceRatePct))
            else -> payment(1, graceRatePct)
        }
        val peakAfter = if (grace < totalMonths) {
            max(payment(grace + 1, postRatePct), payment(max(grace, deferred) + 1, postRatePct))
        } else peakDuring
        val fullRatePeak = payment(deferred + 1, postRatePct)
        return DiffScheduleResult(peakDuring, peakAfter, fullRatePeak,
            payment(totalMonths, if (grace == totalMonths) graceRatePct else postRatePct))
    }

    data class MaxDiffResult(
        val maxByPeakPostGrace: Double,
        val maxByStrictMonth1PostRate: Double
    )

    fun maxDifferentiatedPrincipal(
        maxMonthlyPmt: Double,
        graceRatePct: Double,
        graceMonths: Int,
        postRatePct: Double,
        totalMonths: Int,
        principalGraceMonths: Int = 0
    ): MaxDiffResult {
        if (maxMonthlyPmt <= 0.0 || totalMonths <= 0) return MaxDiffResult(0.0, 0.0)
        val probe = differentiatedDetails(1.0, graceRatePct, graceMonths, postRatePct, totalMonths, principalGraceMonths)
        return MaxDiffResult(
            maxByPeakPostGrace = maxMonthlyPmt / max(probe.month1Grace, probe.peakPostGrace),
            maxByStrictMonth1PostRate = maxMonthlyPmt / probe.month1FullPostRate
        )
    }

    fun maxProgramLimits(
        program: CreditProgram,
        profile: UserFinanceProfile
    ): Triple<Double, Double, Double?> {
        val room = profile.freePdnRoomByn
        return if (program.scheduleType == ScheduleType.DIFFERENTIATED) {
            val dMax = maxDifferentiatedPrincipal(
                room,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths,
                program.principalGraceMonths
            )
            val chosen = if (profile.forceConservativeP0) dMax.maxByStrictMonth1PostRate else dMax.maxByPeakPostGrace
            Triple(min(chosen, program.maxAmountByn), min(dMax.maxByStrictMonth1PostRate, program.maxAmountByn), null)
        } else {
            val aMax = maxSteppedAnnuityPrincipal(
                room,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths
            )
            val diffPeak = maxDifferentiatedPrincipal(
                room,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths
            ).maxByPeakPostGrace
            val chosen = if (profile.forceConservativeP0) aMax.conservativeP0Principal else aMax.maxPostGracePrincipal
            val fallbackDiff = if (program.scheduleType == ScheduleType.ANNUITY_UNSPECIFIED) {
                min(diffPeak, program.maxAmountByn)
            } else {
                null
            }
            Triple(min(chosen, program.maxAmountByn), min(aMax.conservativeP0Principal, program.maxAmountByn), fallbackDiff)
        }
    }

    fun isProgramEligibleForCar(car: CarOffer, program: CreditProgram): Boolean {
        return when (program.scope) {
            CreditScope.GEELY_EV -> car.isGeelyEx
            CreditScope.BEL_AUTO -> {
                if (program.id.contains("dongfeng_top")) {
                    car.isDongfengIceOrForthing
                } else if (program.id.contains("belgee")) {
                    car.isBelPartner && !car.isGeelyEx
                } else {
                    car.isBelPartner || car.isGeelyEx || car.isDongfengIceOrForthing
                }
            }
            CreditScope.EV_ALL -> car.isEv && !car.isUsed && car.isDealerLegalEntity
            CreditScope.EV_UNDER_TEN_YEARS -> car.isEv && (!car.isUsed || car.modelYear?.let {
                Year.now(ZoneId.of("Europe/Minsk")).value - it in 0..9
            } == true)
            CreditScope.PARTNER_ALL -> car.isDealerLegalEntity
            CreditScope.USED_PARTNER -> car.isUsed && car.isDealerLegalEntity
            CreditScope.UNIVERSAL_AUTO -> if (car.isUsed) {
                !program.id.contains("new_only") && (car.isDealerLegalEntity || program.id.contains("any"))
            } else {
                car.isDealerLegalEntity || program.id.contains("any")
            }
            CreditScope.CONSUMER -> true
            CreditScope.BEL_GOODS -> car.isBelPartner || car.isGeelyEx
            CreditScope.LEASING -> car.isDealerLegalEntity
        }
    }

    fun evaluateCarWithProgram(
        car: CarOffer,
        program: CreditProgram,
        profile: UserFinanceProfile
    ): PaymentEvaluation {
        val room = profile.freePdnRoomByn
        val effectivePvByn: Double
        val effectivePvPct: Int

        if (profile.useCustomDownPaymentByn && profile.customDownPaymentByn > 0.0) {
            val minReqByn = car.priceByn * (program.minDownPaymentPct / 100.0)
            effectivePvByn = max(profile.customDownPaymentByn, minReqByn).coerceAtMost(car.priceByn * 0.90)
            effectivePvPct = ((effectivePvByn / car.priceByn) * 100.0).roundToInt()
        } else {
            effectivePvPct = max(profile.downPaymentPct, program.minDownPaymentPct)
            effectivePvByn = car.priceByn * (effectivePvPct / 100.0)
        }

        val loan = (car.priceByn - effectivePvByn).coerceAtLeast(0.0)
        val (maxLoanByPdn, _, diffFallback) = maxProgramLimits(program, profile)
        val minPvFactor = (1.0 - program.minDownPaymentPct / 100.0).coerceAtLeast(0.10)
        val maxPriceMinPv = maxLoanByPdn / minPvFactor
        val maxPrice10Pv = maxLoanByPdn / 0.90

        val graceMonthly: Double
        val peakPost: Double
        val conservativeP0: Double
        val strictDiffM1: Double
        val deciding: Double

        if (program.scheduleType == ScheduleType.DIFFERENTIATED) {
            val d = differentiatedDetails(
                loan,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths,
                program.principalGraceMonths
            )
            graceMonthly = d.month1Grace
            peakPost = d.peakPostGrace
            conservativeP0 = d.month1FullPostRate
            strictDiffM1 = d.month1FullPostRate
            deciding = if (profile.forceConservativeP0) strictDiffM1 else max(graceMonthly, peakPost)
        } else {
            val s = steppedAnnuity(
                loan,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths
            )
            val dStrict = differentiatedDetails(
                loan,
                program.graceRatePct,
                program.graceMonths,
                program.postRatePct,
                program.maxMonths
            )
            graceMonthly = s.graceMonthly
            peakPost = s.postGraceMonthly
            conservativeP0 = s.fullRateFromP0Monthly
            strictDiffM1 = max(dStrict.month1Grace, dStrict.peakPostGrace)
            deciding = if (profile.forceConservativeP0) conservativeP0 else max(graceMonthly, peakPost)
        }

        val margin = room - deciding
        val exceedsBankCap = loan > program.maxAmountByn
        val verdict = when {
            exceedsBankCap -> PdnVerdict.FAIL
            margin >= 0.0 -> PdnVerdict.PASS
            margin >= -25.0 -> PdnVerdict.BORDERLINE
            else -> PdnVerdict.FAIL
        }

        val reason = when {
            exceedsBankCap -> "Сумма кредита превышает лимит программы (${program.maxAmountByn.toInt()} BYN)"
            verdict == PdnVerdict.PASS -> {
                val p0Note = if (!profile.forceConservativeP0 && conservativeP0 > room + 0.5) {
                    " (при жёстком скоринге от полной ставки ${conservativeP0.roundToInt()} BYN — на грани)"
                } else {
                    ""
                }
                "Запас ${margin.roundToInt()} BYN/мес до потолка ${room.roundToInt()} BYN$p0Note"
            }
            verdict == PdnVerdict.BORDERLINE ->
                "Превышение всего ${(-margin).roundToInt()} BYN — легко решается доп. взносом ~${(((-margin) / deciding) * loan).roundToInt()} BYN"
            else -> "Не проходит ПДН на ${(-margin).roundToInt()} BYN/мес (требуется аванс или больший срок)"
        }

        return PaymentEvaluation(
            program = program,
            carPriceByn = car.priceByn,
            appliedDownPaymentPct = effectivePvPct,
            downPaymentByn = effectivePvByn,
            loanPrincipalByn = loan,
            graceMonthlyByn = graceMonthly,
            peakPostGraceMonthlyByn = peakPost,
            conservativeP0MonthlyByn = conservativeP0,
            strictDiffMonth1MonthlyByn = strictDiffM1,
            decidingMonthlyByn = deciding,
            maxLoanByPdnByn = maxLoanByPdn,
            maxCarPriceAtMinPvByn = maxPriceMinPv,
            maxCarPriceAt10PvByn = maxPrice10Pv,
            diffFallbackMaxLoanByn = diffFallback,
            verdict = verdict,
            marginByn = margin,
            reasonRu = reason
        )
    }

    fun totalCreditPayments(evaluation: PaymentEvaluation): Double {
        val program = evaluation.program
        val months = program.maxMonths
        val graceMonths = program.graceMonths.coerceIn(0, months)
        if (program.scheduleType == ScheduleType.DIFFERENTIATED) {
            val principal = evaluation.loanPrincipalByn
            val deferred = program.principalGraceMonths.coerceIn(0, months - 1)
            val monthlyPrincipal = principal / (months - deferred)
            val interest = (1..months).sumOf { month ->
                val opening = principal - monthlyPrincipal * (month - deferred - 1).coerceAtLeast(0)
                val rate = if (month <= graceMonths) program.graceRatePct else program.postRatePct
                opening * rate / 1200
            }
            return principal + interest
        }
        return evaluation.graceMonthlyByn * graceMonths + evaluation.peakPostGraceMonthlyByn * (months - graceMonths)
    }

    fun evaluateAllProgramsForCar(
        car: CarOffer,
        programs: List<CreditProgram>,
        profile: UserFinanceProfile
    ): List<PaymentEvaluation> {
        return programs
            .filter { isProgramEligibleForCar(car, it) }
            .map { evaluateCarWithProgram(car, it, profile) }
            .sortedWith(
                compareBy<PaymentEvaluation> { it.verdict.ordinal }
                    .thenBy { it.decidingMonthlyByn }
            )
    }

    fun bestEvaluationForCar(
        car: CarOffer,
        programs: List<CreditProgram>,
        profile: UserFinanceProfile
    ): PaymentEvaluation? {
        return evaluateAllProgramsForCar(car, programs, profile).firstOrNull()
    }

    fun projectedPrice2027(car: CarOffer, scenario: ForecastScenario): Double {
        val pct = when (scenario) {
            ForecastScenario.OPTIMISTIC_BULL -> car.forecast2027BullPct
            ForecastScenario.BASE -> car.forecast2027BasePct
            ForecastScenario.PESSIMISTIC_BEAR -> car.forecast2027BearPct
        }
        return car.priceByn * (1.0 + pct)
    }

    fun simulateDepositSavings(
        monthlySaveByn: Double,
        months: Int = 10,
        annualRatePct: Double = 12.0
    ): Double {
        if (monthlySaveByn <= 0.0 || months <= 0) return 0.0
        val r = annualRatePct / 100.0 / 12.0
        var total = 0.0
        repeat(months) {
            total = (total + monthlySaveByn) * (1.0 + r)
        }
        return total
    }
}
