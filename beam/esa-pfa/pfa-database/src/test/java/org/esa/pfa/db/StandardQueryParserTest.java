package org.esa.pfa.db;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumericConfig;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.junit.Test;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class StandardQueryParserTest {
    @Test
    public void testRangeParsing() throws Exception {
        NumericConfig numericConfig = new NumericConfig(8, NumberFormat.getNumberInstance(Locale.ENGLISH), FieldType.NumericType.FLOAT);
        HashMap<String, NumericConfig> numericConfigMap = new HashMap<String, NumericConfig>();
        numericConfigMap.put("reflec_7", numericConfig);
        numericConfigMap.put("reflec_8", numericConfig);
        numericConfigMap.put("reflec_9", numericConfig);
        StandardQueryParser parser = new StandardQueryParser();
        parser.setNumericConfigMap(numericConfigMap);

        Query query1 = parser.parse("reflec_8:[0.0 TO 1.0]", "x");
        assertEquals(NumericRangeQuery.class, query1.getClass());

        Query query2 = parser.parse("reflec_8:[0.0 TO 1.0] AND reflec_9:[0.2 TO 0.6]^3.1", "x");
        assertEquals(BooleanQuery.class, query2.getClass());
        BooleanClause clause1 = ((BooleanQuery) query2).getClauses()[0];
        BooleanClause clause2 = ((BooleanQuery) query2).getClauses()[1];
        NumericRangeQuery<Float> nrq1 = NumericRangeQuery.newFloatRange("reflec_8", 8, 0.0F, 1.0F, true, true);
        NumericRangeQuery<Float> nrq2 = NumericRangeQuery.newFloatRange("reflec_9", 8, 0.2F, 0.6F, true, true);
        nrq2.setBoost(3.1F);
        assertEquals(nrq1, clause1.getQuery());
        assertEquals(BooleanClause.Occur.MUST, clause1.getOccur());
        assertEquals(nrq2, clause2.getQuery());
        assertEquals(BooleanClause.Occur.MUST, clause2.getOccur());
    }
}
