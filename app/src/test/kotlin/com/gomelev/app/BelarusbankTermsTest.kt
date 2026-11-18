package com.gomelev.app

import com.gomelev.app.network.BelarusbankTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BelarusbankTermsTest {
    private val page = """
        <tbody>
          <tr><th>Размер кредита</th><td>до 90 % стоимости автомобиля модели Monjaro, электромобили моделей EX2, EX5.</td></tr>
          <tr><th>Процентная ставка</th><td>Другие модели</td>
              <td>при подаче заявки на кредит в отделениях банка</td>
              <td>при подаче заявки на кредит через каналы ДБО<sup>1</sup></td></tr>
          <tr><td>Другая ставка</td><td>Ставка отделения</td><td>
            в течение первых 12 месяцев со дня заключения договора ‒ ставка рефинансирования НБ - 3,75 п.п.;
            начиная с 13-го месяца ‒ ставка рефинансирования НБ + 6,75 п.п.
          </td></tr>
          <tr><th>Срок кредита</th><td>до 10 лет</td></tr>
          <tr><th>Погашение</th><td>основной долг – ежемесячно, равными долями</td></tr>
        </tbody>
    """.trimIndent()

    @Test
    fun `selects the online EX2 and EX5 terms`() {
        assertEquals(BelarusbankTerms(12, -3.75, 6.75, 120, 10), BelarusbankTerms.parse(page))
    }

    @Test
    fun `rejects partial and inconsistent conditions`() {
        assertNull(BelarusbankTerms.parse("<html>503</html>"))
        assertNull(BelarusbankTerms.parse(page.replace("каналы ДБО", "отделение")))
        assertNull(BelarusbankTerms.parse(page.replace("13-го месяца", "15-го месяца")))
    }
}
