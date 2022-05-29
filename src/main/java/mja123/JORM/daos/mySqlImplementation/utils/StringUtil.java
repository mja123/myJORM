package mja123.JORM.daos.mySqlImplementation.utils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class StringUtil {
  public static ArrayList<String> attributeToColumn(ConcurrentHashMap<String, Object> entity) {
    ArrayList<String> formattedFields = new ArrayList<>();

    entity.forEach(
        (k, v) -> {
          // Search fields that contains uppercase letters and replaces it for the underscore + the
          // lowercase letter

          for (Character character : k.toCharArray()) {
            if (Character.isUpperCase(character)) {
              k =
                  k.replace(
                      character.toString(), "_" + character.toString().toLowerCase(Locale.ROOT));
            }
          }

          formattedFields.add(k);
        });

    return formattedFields;
  }

  public static void columnsToAttributes(ConcurrentHashMap<String, Object> fieldsResultSet) {
    fieldsResultSet.forEach(
        (k, v) -> {
          if (k.contains("_")) {
            // The index of the letter after the underscore (first letter in the joined word).
            int indexOfNextWord = k.indexOf("_") + 1;
            char firstLetter = k.charAt(indexOfNextWord);

            String formattedField =
                k.replace("_" + firstLetter, Character.toString(firstLetter).toUpperCase());
            fieldsResultSet.put(formattedField, v);
            fieldsResultSet.remove(k);
          }
        });
  }
}
