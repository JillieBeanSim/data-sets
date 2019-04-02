/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */
package org.zowe.unix.files.services.zosmf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseCache;
import org.zowe.unix.files.exceptions.NotAFileException;

import com.google.gson.JsonObject;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class GetUnixFileChtagRunner extends AbstractZosmfUnixFilesRequestRunner<String>{
    
    private String path;

    @Override
    protected int[] getSuccessStatus() {
        return new int[] { HttpStatus.SC_OK };
    }

    @Override
    protected RequestBuilder prepareQuery(ZosmfConnector zosmfConnector) throws URISyntaxException, IOException {
        URI requestUrl = zosmfConnector.getFullUrl("restfiles/fs" + path);
        StringEntity requestEntity = new StringEntity("{ \"request\": \"chtag\", \"action\": \"list\" }");
        RequestBuilder requestBuilder = RequestBuilder.put(requestUrl).setEntity(requestEntity);
        requestBuilder.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        return requestBuilder;
    }

    @Override
    protected String getResult(ResponseCache responseCache) throws IOException {
        String codepage = responseCache.getEntityAsJsonObject().get("stdout").getAsString();
        return codepage;
    }

    @Override
    protected ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) throws IOException {
        if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            String category = jsonResponse.get("category").toString();
            String returnCode = jsonResponse.get("rc").toString();
            //No helpful message in response so check category and status code
            if (null != category && category.equals("7") && null != returnCode && returnCode.equals("266")){
               throw new NotAFileException(path);
            }
        }
        return createUnixFileException(jsonResponse, statusCode, path);
    }
}
