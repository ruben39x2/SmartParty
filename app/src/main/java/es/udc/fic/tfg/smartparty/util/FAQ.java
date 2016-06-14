/**
 * Copyright 2016 Rubén Montero Vázquez
 *
 * This file is part of Smart Party.
 *
 * Smart Party is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smart Party is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smart Party.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.udc.fic.tfg.smartparty.util;

/**
 * POJO simple AF.
 *
 * @author Rubén Montero Vázquez
 */
public class FAQ {
   private String question;
   private String answer;

   public FAQ(String question, String answer) {
      this.question = question;
      this.answer = answer;
   }

   public String getQuestion() {
      return question;
   }

   public String getAnswer() {
      return answer;
   }
}
