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

package com.exadel.aem.backpack.core.datasource;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.exadel.aem.backpack.core.services.PackageService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import java.util.List;
import java.util.stream.Collectors;

import static javax.jcr.query.Query.JCR_SQL2;

/**
 * Servlet that implements {@code datasource} pattern for populating a TouchUI {@code select} widget
 * with present packages' groups' names
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=backpack/data-sources/group-dynamic-select",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        }
)
@SuppressWarnings("PackageAccessibility")
// because Servlet and HttpServletResponse classes reported as a non-bundle dependency
public class GroupDynamicSelectDataSource extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GroupDynamicSelectDataSource.class);

    @SuppressWarnings("squid:S1075") // this JCR path is static by design
    private static final String DEFAULT_PATH_KEY = "/etc/packages/backpack";
    private static final String ROOT_KEY = "/etc/packages";

    private static final String SELECT_STATEMENT = "SELECT * FROM [sling:Folder] AS node WHERE ISCHILDNODE(node, '/etc/packages') order by node.[jcr:path] asc";
    private static final String ROOT_TEXT = "All packages";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // injected value
    private transient DataSourceBuilder dataSourceBuilder;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // injected value
    private transient QueryHelper queryHelper;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // injected value
    private transient PackageService packageService;

    /**
     * Processes {@code GET} requests to the current endpoint to add to the {@code SlingHttpServletRequest}
     * a {@code datasource} object filled with names of present packages' groups
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        Resource datasourceResource = resolver.getResource(request.getRequestPathInfo().getResourcePath() + "/datasource");
        boolean useTitleAsValue = datasourceResource != null
                && datasourceResource.getValueMap().getOrDefault("valueMember", StringUtils.EMPTY).equals("title");
        try {
            List<Resource> results = queryHelper.findResources(resolver, JCR_SQL2, SELECT_STATEMENT, StringUtils.EMPTY);
            List<DataSourceOption> options = results.stream()
                    .map(resource -> createDataOption(resource, useTitleAsValue))
                    .collect(Collectors.toList());
            RequestParameter groupParam = request.getRequestParameter("group");
            if (groupParam != null) {
                DataSourceOption firstDataSourceOption = new DataSourceOption(getOptionText(groupParam.getString()), groupParam.getString());
                options.add(0, firstDataSourceOption);
            } else {
                DataSourceOption firstDataSourceOption = new DataSourceOption(getOptionText(DEFAULT_PATH_KEY), DEFAULT_PATH_KEY);
                options.add(0, firstDataSourceOption);
            }
            DataSourceOption rootDataSourceOption = new DataSourceOption(ROOT_TEXT, ROOT_KEY);
            options.add(1, rootDataSourceOption);
            dataSourceBuilder.addDataSource(request, options);
        } catch (RepositoryException e) {
            LOG.error("Unable to collect the information to populate the dynamic-select drop-down.", e);
        }
    }

    /**
     * Called from {@link GroupDynamicSelectDataSource#doGet(SlingHttpServletRequest, SlingHttpServletResponse)} to
     * map a {@code Resource} containing datasource option requisites to a {@code DataSourceOption} instance
     *
     * @param resource {@code Resource} object
     * @param useTitleAsValue True to use option text (without a path) as option value; otherwise, false
     * @return {@code DataSourceOption} object
     */
    private DataSourceOption createDataOption(Resource resource, boolean useTitleAsValue) {
        return new DataSourceOption(
                getOptionText(resource.getPath()),
                useTitleAsValue ? getOptionText(resource.getPath()) : resource.getPath()
        );
    }

    /**
     * Gets {@code datasource} option label by returning the trailing element of an underlying JCR path
     *
     * @param resourcePath String representing a JCR path
     * @return String value
     */
    private String getOptionText(String resourcePath) {
        String[] path = resourcePath.split("/");
        return path[path.length - 1];
    }
}
