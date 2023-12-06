package org.vufind.util;

import java.util.EnumSet;

import org.solrmarc.index.extractor.formatter.FieldFormatter;
import org.solrmarc.tools.DataUtil;

public class TitleNormalizer implements Normalizer
{
    @Override
    public byte[] normalize(String s)
    {
        EnumSet<FieldFormatter.eCleanVal> cleanValue = DataUtil.getCleanValForParam("titleSortLower");
        String normalizedTitle = DataUtil.cleanByVal(s, cleanValue);
        byte[] bytes = normalizedTitle == null ? null : normalizedTitle.getBytes();
        return bytes;
    }
}
