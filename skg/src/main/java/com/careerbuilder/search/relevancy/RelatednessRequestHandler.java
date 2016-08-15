/**Copyright 2015-2016 CareerBuilder, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
**/

package com.careerbuilder.search.relevancy;

import com.careerbuilder.search.relevancy.model.ParameterSet;
import com.careerbuilder.search.relevancy.model.RelatednessRequest;
import com.careerbuilder.search.relevancy.model.RelatednessResponse;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;


public class RelatednessRequestHandler extends RequestHandlerBase
{
    private ResourceLoader loader;

    @Override
    public void init(NamedList args) {
        super.init(args);
    }

    @Override
    public void handleRequestBody(SolrQueryRequest solrReq, SolrQueryResponse solrRsp)
            throws Exception {
        RelatednessRequest request = parsePost(solrReq);
        new RequestValidator(solrReq, request).validate();
        ParameterSet parameterSet = new ParameterSet(solrReq.getParams(), defaults, invariants);
        NodeContext context = new NodeContext(request, solrReq, parameterSet);
        RequestTreeRecurser recurser = new RequestTreeRecurser(context);
        RelatednessResponse response = new RelatednessResponse();
        response.data = recurser.score();
        solrRsp.add("relatednessResponse", response);
    }

    private RelatednessRequest parsePost(SolrQueryRequest request) throws IOException {
        String inputString = getPostString(request);
        try {
            return new Gson().fromJson(inputString, RelatednessRequest.class);
        } catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e.getMessage());
        }
    }

    private String getPostString(SolrQueryRequest request) throws IOException {
        Reader inputReader = null;
        Iterable<ContentStream> streams = request.getContentStreams();
        if (streams != null) {
            Iterator<ContentStream> iter = streams.iterator();
            if (iter.hasNext()) {
                inputReader = iter.next().getReader();
            }
            if (iter.hasNext()) {
                throwWithClassName(" does not support multiple ContentStreams");
            }
        }
        if (inputReader == null) {
            throwWithClassName(" requires POST data");
        }
        String inputString;
        inputString = CharStreams.toString(inputReader);
        inputReader.close();
        if(inputString.equals("") || inputString == null) {
           throwWithClassName(" requires POST data");
        }
        return inputString;
    }

    private void throwWithClassName(String msgAfterClassName) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                getClass().getSimpleName()+ msgAfterClassName);
    }

    //////////////////////// SolrInfoMBeans methods //////////////////////

    @Override
    public String getDescription()
    {
        return "Scores fields using passed in foreground/background";
    }

    @Override
    public String getSource() {
        return "$Source$";
    }

    @Override
    public String getVersion() {
        return "$Revision$";
    }


}