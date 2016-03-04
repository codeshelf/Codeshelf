package com.codeshelf.behavior;

import static net.sf.dynamicreports.report.builder.DynamicReports.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;



import com.codeshelf.behavior.OrderBehavior.OrderDetailView;
import com.codeshelf.model.domain.OrderHeader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class PrintBehavior {

	public StreamingOutput printOrder(String script, OrderHeader orderHeader, List<OrderDetailView> details) throws JRException, ScriptException {
		JasperReportBuilder reportBuilder  = eval(script);
		 StreamingOutput out = doReport(reportBuilder, details, "test");
		 return out;
	}
	
	private JasperReportBuilder  eval(String script) throws ScriptException {
		JasperReportBuilder reportBuilder = report();
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy");
		reportBuilder = (JasperReportBuilder) engine.eval(script, new SimpleBindings(Maps.newHashMap(ImmutableMap.of("reportBuilder", reportBuilder))));
		return reportBuilder;
	}
	
	private StreamingOutput doReport(final JasperReportBuilder inReport, final Collection<?> inData, String name) throws JRException {
		JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(inData);
		return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					
					inReport.setDataSource(source);
					inReport.toPdf(new FileOutputStream("/tmp/tmpA.pdf"));
				} catch (DRException e) {
					throw new WebApplicationException(e);
				}
				output.close();
			}
		};
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