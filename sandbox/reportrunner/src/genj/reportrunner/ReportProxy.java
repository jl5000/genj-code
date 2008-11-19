package genj.reportrunner;

import genj.fo.Document;
import genj.fo.Format;
import genj.option.PropertyOption;
import genj.report.Report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy class for a report. This class is used to access report options and to run a report.
 * One proxy object is associated with one report.
 *
 * @author Przemek Wiech <pwiech@losthive.org>
 * @version $Id: ReportProxy.java,v 1.2 2008-11-19 10:03:28 pewu Exp $
 */
public class ReportProxy
{
	/**
	 * The modified report object. Used for running the report.
	 * See ReportProxyFactory for details.
	 */
    private Report proxiedReport;

    /**
     * Original unmodified report object. Used for translations.
     */
    private Report originalReport;

    /**
     * Report options.
     */
    private Map<String, ReportOption> options = new HashMap<String, ReportOption>();

    /**
     * Name of output file.
     */
    private String outputFileName = null;

    /**
     * Format of output file.
     */
    private String outputFormat = null;

    /**
     * Report output.
     */
	PrintWriter out = null;

    /**
     * Available formats for FO-based reports.
     */
    private static final Map<String, Format> FORMATS = new HashMap<String, Format>();
    static
    {
        for (Format format : Format.getFormats())
            FORMATS.put(format.getFormat().toLowerCase(), format);
    }

    /**
     * Initializes the object.
     * @param originalReport original report object
     * @param proxiedReport  modified report object
     */
    public ReportProxy(Report originalReport, Report proxiedReport)
    {
        this.proxiedReport = proxiedReport;
        this.originalReport = originalReport;

        @SuppressWarnings("unchecked")
        List<PropertyOption> properties = proxiedReport.getOptions();
        for (PropertyOption property : properties)
        {
            String propertyName = property.getProperty();
            String description = originalReport.translate(propertyName);
            options.put(propertyName, new ReportOption(property, description));
        }
    }

    /**
     * Generates the output file.
     * Overrides Report.showDocumentToUser().
     * @param doc  document created by the report
     */
    public void showDocumentToUser(Document doc)
    {
    	closeOut();
        Format formatter = FORMATS.get(outputFormat);

        File file = new File(outputFileName);
        try
        {
            formatter.format(doc, file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns output file.
     * Overrides Report.getFileFromUser()
     */
    public File getFileFromUser()
    {
    	closeOut();
        return new File(outputFileName);
    }

    private void closeOut()
    {
    	if (out != null)
    	{
    		out.close();
    		setOutput(null);
    	}
    }

    /**
     * Returns a translated string.
     * Overrides Report.translate().
     */
    public String translate(String key, Object[] values)
    {
        return originalReport.translate(key, values);
    }

    /**
     * Runs the report.
     * Overrides Report.start().
     * @param o  initializing object
     */
    public void start(Object o) throws ReportProxyException
    {
    	if (outputFileName != null)
    	{
    		out = new PrintWriter(new OnDemandFileWriter(outputFileName));
			setOutput(out);
    	}
        try
        {
            proxiedReport.start(o);
        }
        catch (Throwable t)
        {
            throw new ReportProxyException(t);
        }
        finally
        {
        	if (out != null)
        	{
        		out.close();
        		setOutput(null);
        	}
        }
    }

    private void setOutput(PrintWriter printWriter)
    {
    	try
    	{
    		out = printWriter;
			Method method = proxiedReport.getClass().getDeclaredMethod("setOutput", PrintWriter.class);
			method.invoke(proxiedReport, printWriter);
		}
		// None of these should ever happen
    	catch (NoSuchMethodException e)
    	{
			e.printStackTrace();
		}
    	catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
    	catch (IllegalAccessException e)
    	{
			e.printStackTrace();
		}
    	catch (InvocationTargetException e)
    	{
			e.printStackTrace();
		}
    }

    /**
     * Returns report options.
     */
    public Collection<ReportOption> getOptions()
    {
        return options.values();
    }

    /**
     * Sets a report option.
     */
    public void setOption(String key, String value) throws ReportProxyException
    {
        ReportOption option = options.get(key);
        if (option == null)
            throw new ReportProxyException("Unknown option '" + key + "' in report " + proxiedReport.getClass().getCanonicalName());
        option.setValue(value);
    }

    /**
     * Resets all options to default values.
     */
    public void resetOptions()
    {
        for (ReportOption option : options.values())
            option.reset();
        outputFormat = null;
        outputFileName = null;
    }

    /**
     * Sets output file name.
     */
    public void setOutputFileName(String outputFileName)
    {
        this.outputFileName = outputFileName;
    }

    /**
     * Sets output file format for FO-based reports.
     */
    public void setOutputFormat(String outputFormat)
    {
        this.outputFormat = outputFormat;
    }

    /**
     * Returns available file formats for FO-based reports.
     * @return
     */
    public static Collection<String> getFormats()
    {
        return FORMATS.keySet();
    }
}
