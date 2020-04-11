/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component(service = Servlet.class,
        property = {
                "sling.servlet.paths=" + "/services/backpack/buildPackage",
                "sling.servlet.methods=[get,post]",

        })
public class BuildPackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();
    public static final String APPLICATION_JSON = "application/json";

    @Reference
    private transient PackageService packageService;

    @Reference
    private transient RequestAdapter requestAdapter;

    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON);
        ValidatorResponse<BuildPackageModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), BuildPackageModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse.getLog()));
        } else {
            PackageInfo packageInfo;
            BuildPackageModel model = validatorResponse.getModel();
            if (model.isTestBuild()) {
                packageInfo = packageService.testBuildPackage(request.getResourceResolver(), model);
            } else {
                packageInfo = packageService.buildPackage(request.getResourceResolver(), model);
            }
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws IOException {
        ValidatorResponse<LatestPackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), LatestPackageInfoModel.class);

        response.setContentType(APPLICATION_JSON);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            final PackageInfo latestPackageBuildInfo = packageService.getLatestPackageBuildInfo(validatorResponse.getModel());
            response.getWriter().write(GSON.toJson(latestPackageBuildInfo));
        }
    }
}

