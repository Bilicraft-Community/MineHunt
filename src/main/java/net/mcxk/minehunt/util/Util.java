package net.mcxk.minehunt.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Util {
    /**
     * Convert strList to String. E.g "Foo, Bar"
     *
     * @param strList Target list
     * @return str
     */
    @NotNull
    public static String list2String(@NotNull List<String> strList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strList.size(); i++) {
            builder.append(strList.get(i));
            if (i + 1 != strList.size()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
}
