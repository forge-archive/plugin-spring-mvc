package org.jboss.forge.scaffold.spring.metawidget.inspectionresultprocessor;

import static org.metawidget.inspector.InspectionResultConstants.TRUE;

import java.util.Map;

import org.metawidget.inspectionresultprocessor.impl.BaseInspectionResultProcessor;
import org.metawidget.statically.StaticXmlWidget;

public class SearchResultInspectionResultProcessor
        extends BaseInspectionResultProcessor<StaticXmlWidget>
{
    //
    // Protected methods
    //

    @Override
    protected void processAttributes(Map<String, String> attributes, StaticXmlWidget metawidget)
    {
        attributes.put("search-result", TRUE);
    }
}
