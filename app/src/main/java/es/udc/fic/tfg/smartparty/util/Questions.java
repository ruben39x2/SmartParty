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

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;

/**
 * Class with an unique static method that returns a list with some {@link FAQ}
 *
 * @see es.udc.fic.tfg.smartparty.activity.AboutActivity
 * @author Rubén Montero Vázquez
 */
public class Questions {
   public static List<FAQ> get(Context context) {
      int NUMBER_OF_FAQS = 7;
      List<FAQ> list = new ArrayList<>(NUMBER_OF_FAQS);
      list.add(new FAQ(context.getString(R.string.question1), context.getString(R.string.answer1)));
      list.add(new FAQ(context.getString(R.string.question3), context.getString(R.string.answer3)));
      list.add(new FAQ(context.getString(R.string.question4), context.getString(R.string.answer4)));
      list.add(new FAQ(context.getString(R.string.question5), context.getString(R.string.answer5)));
      list.add(new FAQ(context.getString(R.string.question6), context.getString(R.string.answer6)));
      list.add(new FAQ(context.getString(R.string.question7), context.getString(R.string.answer7)));
      list.add(new FAQ(context.getString(R.string.question8), context.getString(R.string.answer8)));
      list.add(new FAQ(context.getString(R.string.question2), context.getString(R.string.answer2)));
      return list;
   }
}
