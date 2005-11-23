//$Header$
/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jmeter.testelement;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jmeter.report.DataSet;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.SamplingStatCalculator;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * @author Peter Lin
 *
 * The purpose of TableData is to contain the results of a single .jtl file.
 * It is equivalent to what the AggregateListener table. A HashMap is used
 * to store the data. The URL is the key and the value is SamplingStatCalculator
 */
public class JTLData implements Serializable, DataSet {

    private static final Logger log = LoggingManager.getLoggerForClass();
    
    protected HashMap data = new HashMap();
    protected String jtl_file = null;
    protected long startTimestamp = 0;
    protected long endTimestamp = 0;
    protected File inputFile = null;
    
	/**
	 * 
	 */
	public JTLData() {
		super();
	}

    /**
     * Return a Set of the URL's
     * @return
     */
    public Set getURLs() {
        return this.data.keySet();
    }
    
    /**
     * Return a Set of the values
     * @return
     */
    public Set getStats() {
        return this.data.entrySet();
    }
    
    /**
     * The purpose of the method is to make it convienant to pass a list
     * of the URL's and return a list of the SamplingStatCalculators. If
     * no URL's match, the list is empty.
     * The SamplingStatCalculators will be returned in the same sequence
     * as the url list.
     * @param urls
     * @return
     */
    public List getStats(List urls) {
        ArrayList items = new ArrayList();
        Iterator itr = urls.iterator();
        if (itr.hasNext()) {
            SamplingStatCalculator row = (SamplingStatCalculator)itr.next();
            if (row != null) {
                items.add(row);
            }
        }
        return items;
    }
    
    public void setDataSource(String absolutePath) {
        this.jtl_file = absolutePath;
    }
    
    public String getDataSource() {
        return this.jtl_file;
    }
    
    public String getDataSourceName() {
        if (inputFile == null) {
            inputFile = new File(getDataSource());
        }
        return inputFile.getName().substring(0,inputFile.getName().length() - 4);
    }
    
    public void setStartTimestamp(long stamp) {
        this.startTimestamp = stamp;
    }
    
    public long getStartTimestamp() {
        return this.startTimestamp;
    }
    
    public void setEndTimestamp(long stamp) {
        this.endTimestamp = stamp;
    }
    
    public long getEndTimestamp() {
        return this.endTimestamp;
    }
    
    /**
     * The date we use for the result is the start timestamp. The
     * reasoning is that a test may run for a long time, but it
     * is most likely scheduled to run using CRON on unix or
     * scheduled task in windows.
     * @return
     */
    public Date getDate() {
        return new Date(this.startTimestamp);
    }
    
    public String getMonthDayDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(this.startTimestamp);
        return String.valueOf(cal.get(Calendar.MONTH)) + " - " + 
        String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }
    
    public String getMonthDayYearDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(this.startTimestamp);
        return String.valueOf(cal.get(Calendar.MONTH)) + " - " + 
            String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + " - " +
            String.valueOf(cal.get(Calendar.YEAR));
    }

    /**
     * The method will SamplingStatCalculator for the given URL. If the URL
     * doesn't exist, the method returns null.
     * @param url
     * @return
     */
    public SamplingStatCalculator getStatistics(String url) {
        if (this.data.containsKey(url)) {
            return (SamplingStatCalculator)this.data.get(url);
        } else {
            return null;
        }
    }

    /**
     * The implementation loads a single .jtl file and cleans up the
     * ResultCollector.
     */
    public void loadData() {
        if (this.getDataSource() != null) {
            ResultCollector rc = new ResultCollector();
            rc.setFilename(this.getDataSource());
            rc.setListener(this);
            try {
                rc.loadExistingFile();
            } catch (IOException e) {
                log.warn(e.getMessage());
                // e.printStackTrace();
            } finally {
                // we clean up the ResultCollector to make sure there's
                // no slow leaks
                rc.clear();
                rc.setListener(null);
                rc = null;
            }
        }
    }
    
    /**
     * the implementation will set the start timestamp if the HashMap
     * is empty. otherwise it will set the end timestamp using the
     * end time
     */
    public void add(SampleResult sample) {
        if (data.size() == 0) {
            this.startTimestamp = sample.getTimeStamp();
        } else {
            this.endTimestamp = sample.getTimeStamp() + sample.getTime();
        }
        // now add the samples to the HashMap
        String url = sample.getSampleLabel();
        if (url == null) {
            url = sample.getURL().toString();
        }
        SamplingStatCalculator row = (SamplingStatCalculator)data.get(url);
        if (row == null) {
            row = new SamplingStatCalculator();
            // just like the aggregate listener, we use the sample label to represent
            // a row. in this case, we use it as a key.
            this.data.put(url,row);
        }
        row.addSample(sample);
    }
    
    /**
     * By default, the method always returns true. Subclasses can over
     * ride the implementation.
     */
    public boolean isStats() {
        return true;
    }
}
