package com.codeshelf.behavior;

import static net.sf.dynamicreports.report.builder.DynamicReports.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.codeshelf.behavior.OrderBehavior.OrderDetailView;
import com.codeshelf.model.domain.OrderHeader;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class PrintBehavior {
	
	private Cache<String, byte[]> cache;

	@Inject
	public PrintBehavior(Cache<String, byte[]> cache) {
		this.cache = cache;
	}

	public String printOrder(String script, OrderHeader orderHeader, List<OrderDetailView> details) throws IOException, ScriptException {
		JasperReportBuilder reportBuilder  = eval(script);
		 String token = doReport(reportBuilder, details, "test");
		 return token;
	}
	
	public Optional<byte[]> getReport(String token) {
		return Optional.fromNullable(cache.getIfPresent(token));
	}
	
	private JasperReportBuilder  eval(String script) throws ScriptException {
		JasperReportBuilder reportBuilder = report();
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy");
		reportBuilder = (JasperReportBuilder) engine.eval(script, new SimpleBindings(Maps.newHashMap(ImmutableMap.of("reportBuilder", reportBuilder))));
		return reportBuilder;
	}
	
	private String doReport(final JasperReportBuilder inReport, final Collection<?> inData, String name) throws IOException {
		JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(inData);
		inReport.setDataSource(source);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			inReport.toPdf(out);
			out.close();
			String token = UUID.randomUUID().toString();
			cache.put(token, out.toByteArray());
			return token;
		} catch (DRException e) {
			throw new IOException(e);
		}
	}
}
/*
import static net.sf.dynamicreports.report.builder.DynamicReports.*;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.component.HorizontalListBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;

   public class ItemExpression extends AbstractSimpleExpression<String> {
      private static final long serialVersionUID = 1L;
      @Override
      public String evaluate(ReportParameters reportParameters) {

         return reportParameters.getValue("itemId");

      }

   }

 

   public class BarcodeExpression extends AbstractSimpleExpression<String> {
      private static final long serialVersionUID = 1L;
      @Override
      public String evaluate(ReportParameters reportParameters) {
         return reportParameters.getValue("itemId");
      }
   }

itemCmp = cmp.verticalList(
//               cmp.text(new ItemExpression()),
               bcode.code128(new BarcodeExpression()).setFixedHeight(24))

reportBuilder
.fields(
            field("itemId", String.class))

.columns(//add columns

          //            title,     field name     data type

          col.componentColumn("Item", itemCmp),

          col.column("UOM",   "uom",  type.stringType()),

          col.column("description", "description", type.stringType()),

          col.column("QTY", "planQuantity", type.integerType()),
          col.column("Loc.", "preferredLocation", type.stringType())
          )
         .title(cmp.text("Getting started"))//shows report title

           .pageFooter(cmp.pageXofY())//shows number of page at page footer
*/