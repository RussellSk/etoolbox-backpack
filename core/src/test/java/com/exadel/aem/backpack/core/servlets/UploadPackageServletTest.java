package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.JcrPackageWrapper;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.pckg.PackageInfoService;
import com.exadel.aem.backpack.core.services.pckg.UploadPackageService;
import com.exadel.aem.backpack.core.services.pckg.impl.PackageInfoServiceImpl;
import com.exadel.aem.backpack.core.services.pckg.impl.UploadPackageServiceImpl;
import com.exadel.aem.backpack.request.RequestAdapter;
import com.exadel.aem.backpack.request.impl.RequestAdapterImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadPackageServletTest {

    private static final String PARAM_FORCE_UPDATE = "forceUpdate";
    private static final String PARAM_FILEUPLOAD = "fileupload";

    @Rule
    public final AemContext context = new AemContext();
    private final UploadPackageServiceImpl uploadPackageService = mock(UploadPackageServiceImpl.class);
    private UploadPackageServlet servlet;

    @Before
    public void beforeTest() {
        context.registerService(UploadPackageService.class, uploadPackageService);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new UploadPackageServlet());
    }

    @Test
    public void shouldReturnBadRequest_whenPackageServiceGetPackageInfoReturnNull() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getSuccessJcrPackageWrapper());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"packageName\":\"some-name.zip\",\"version\":\"some-version\",\"packageStatus\":\"BUILT\",\"packagePath\":\"/some/path\",\"referencedResources\":{},\"log\":[]}", context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnSuccessRequest() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getSuccessJcrPackageWrapper());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"packageName\":\"some-name.zip\",\"version\":\"some-version\",\"packageStatus\":\"BUILT\",\"packagePath\":\"/some/path\",\"referencedResources\":{},\"log\":[]}", context.response().getOutputAsString());
    }


    @Test
    public void shouldReturnBadRequest_whenRequestIsEmpty() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getErrorJcrPackageWrapper());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"message\":\"An incorrect value of parameter(s)\",\"statusCode\":409}", context.response().getOutputAsString());
    }

    private JcrPackageWrapper getErrorJcrPackageWrapper() {
        JcrPackageWrapper jcrPackageWrapper = new JcrPackageWrapper();
        jcrPackageWrapper.setMessage("An incorrect value of parameter(s)");
        jcrPackageWrapper.setStatusCode(SC_CONFLICT);

        return jcrPackageWrapper;
    }

    private JcrPackageWrapper getSuccessJcrPackageWrapper() {

        JcrPackageWrapper jcrPackageWrapper = new JcrPackageWrapper();
        jcrPackageWrapper.setMessage("An incorrect value of parameter(s)");
        jcrPackageWrapper.setStatusCode(SC_CONFLICT);
        jcrPackageWrapper.setPackageInfo(getPackageInfoObj());

        return jcrPackageWrapper;
    }

    private PackageInfo getPackageInfoObj() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackagePath("/some/path");
        packageInfo.setPackageName("some-name.zip");
        packageInfo.setPackageStatus(PackageStatus.BUILT);
        packageInfo.setVersion("some-version");

        return packageInfo;
    }
}