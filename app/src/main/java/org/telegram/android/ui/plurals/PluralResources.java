package org.telegram.android.ui.plurals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import android.content.res.Resources;
import android.content.res.Resources.*;
import android.os.Build;

@SuppressWarnings("nls")
public class PluralResources {
    private Resources resources;
    private Method getResourceBagTextMethod;
    private PluralRules rules;
    private String language;
    private boolean treatZero = true;

    public PluralResources(String language, Resources resources) throws SecurityException, NoSuchMethodException {
        this.resources = resources;
        this.language = language;
        this.rules = PluralRules.ruleForLocale(new Locale(language));
        getResourceBagTextMethod = resources.getAssets().getClass().getDeclaredMethod("getResourceBagText", int.class, int.class);
        getResourceBagTextMethod.setAccessible(true);
    }

    public void setTreatZero(boolean treatZero) {
        this.treatZero = treatZero;
    }

    /**
     * Return the string value associated with a particular resource ID for a particular
     * numerical quantity, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}. It will be
     * stripped of any styled text information.
     * {@more}
     *
     * @param id         The desired resource identifier, as generated by the aapt
     *                   tool. This integer encodes the package, type, and resource
     *                   entry. The value 0 is an invalid identifier.
     * @param quantity   The number used to get the correct string for the current language's
     *                   plural rules.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return String The string data associated with the resource,
     *         stripped of styled text information.
     * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
     */
    public String getQuantityString(int id, int quantity, Object... formatArgs) throws NotFoundException {
        return String.format(resources.getConfiguration().locale, getQuantityString(id, quantity), formatArgs);
    }

    public String getQuantityString(int id, int quantity) throws NotFoundException {
        // Android 3.0 and later have fixed the problem with plurals,
        // may consider to use system function, will lose special handling of ZERO though
        // if ( Build.SDK_INT >= 11 )
        //    return resources.getQuantityString(id, quantity);

        if (rules == null)
            return resources.getQuantityString(id, quantity);

        if (getResourceBagTextMethod == null)
            throw new IllegalArgumentException();

        Object format = null;
        try {
            // special case -- if translator added special rule for ZERO we disregard language rules
            if (quantity == 0 && treatZero)
                format = getResourceBagTextMethod.invoke(resources.getAssets(), new Object[]{id, PluralRules.ID_ZERO});

            if (format == null)
                format = getResourceBagTextMethod.invoke(resources.getAssets(), new Object[]{id, PluralRules.attrForQuantity(rules.quantityForNumber(quantity))});

            if (format == null)
                format = getResourceBagTextMethod.invoke(resources.getAssets(), new Object[]{id, PluralRules.ID_OTHER});

        } catch (IllegalArgumentException e) {
            throw new NotFoundException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new NotFoundException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new NotFoundException(e.getMessage());
        }

        if (format == null) {
            throw new android.content.res.Resources.NotFoundException("Plural resource ID #0x" + Integer.toHexString(id)
                    + " quantity=" + quantity
                    + " item=" + PluralRules.stringForQuantity(rules.quantityForNumber(quantity)));
        }

        return format.toString();
    }
}
