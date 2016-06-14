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

import android.support.annotation.NonNull;

/**
 * Class used to store in preferences the name of the lists used to filter other users an
 * configure our privacy.
 *
 * This is just an string of the form TGymPeople, for instance, which means that GymPeople list
 * is enabled, or FSchool, which means that School list is disabled.
 *
 * Those "compound" names are store in preferences.
 *
 * @author Rubén Montero Vázquez
 */
public class FilterListName implements Comparable<FilterListName> {
   private String name;
   private boolean active;

   public FilterListName(String name, boolean active) {
      this.name = name;
      this.active = active;
   }

   public FilterListName(String compoundName) {
      this.name = compoundName.substring(1);
      this.active = compoundName.startsWith("T");
   }

   public String getCompoundName() {
      String result = "";
      if (active) result += "T";
      else result += "F";
      result += name;
      return result;
   }

   public String getName() {
      return name;
   }

   public boolean isActive() {
      return active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public int compareTo(@NonNull FilterListName another) {
      return this.getName().compareTo(another.getName());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      FilterListName that = (FilterListName) o;

      return !(name != null ? !name.equals(that.name) : that.name != null);
   }
}
