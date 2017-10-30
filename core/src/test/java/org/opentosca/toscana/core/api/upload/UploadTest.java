package org.opentosca.toscana.core.api.upload;

import java.io.IOException;

import org.opentosca.toscana.core.BaseJUnitTest;
import org.opentosca.toscana.core.Main;
import org.opentosca.toscana.core.testdata.TestCsars;
import org.opentosca.toscana.core.testutils.CategoryAwareJUnitRunner;
import org.opentosca.toscana.core.testutils.TestCategories;
import org.opentosca.toscana.core.testutils.TestCategory;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import retrofit2.Response;
import retrofit2.Retrofit;

import static org.junit.Assert.fail;

@TestCategory(TestCategories.SLOW)
@RunWith(CategoryAwareJUnitRunner.class)
public class UploadTest extends BaseJUnitTest {

    private Thread springThread;

    private TOSCAnaUploadInterface api;

    @Before
    public void setUp() throws Exception {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://127.0.0.1:8091/").build();

        api = retrofit.create(TOSCAnaUploadInterface.class);

        springThread = new Thread(() -> Main.main(new String[]{
            "--datadir=" + tmpdir.getAbsolutePath(),
            "--spring.profiles.active=controller_test",
            "--server.port=8091"
        }));
        springThread.start();
    }

    @Test(timeout = 30000)
    public void testFileUpload() throws Exception {
        waitForServerToStart();
        System.err.println("Server started!");

        RequestBody file = RequestBody.create(MediaType.parse("multipart/form-data"),
            TestCsars.CSAR_YAML_VALID_DOCKER_SIMPLETASK);

        MultipartBody.Part p = MultipartBody.Part.createFormData("file", "test.csar", file);

        Response<ResponseBody> response = api.upload(p, "test-archive").execute();
        if (response.code() != 200) {
            ResponseBody b = response.errorBody();
            System.out.println(b.string());
            fail();
        }
    }

    @Test(timeout = 30000)
    public void testFileUploadFail() throws Exception {
        waitForServerToStart();
        System.err.println("Server started");

        RequestBody file = RequestBody.create(MediaType.parse("multipart/form-data"),
            TestCsars.CSAR_YAML_INVALID_ENTRYPOINT_AMBIGUOUS);

        MultipartBody.Part p = MultipartBody.Part.createFormData("file", "test.csar", file);

        Response<ResponseBody> response = api.upload(p, "test-archive-fail").execute();
        if (response.code() != 200) {
            ResponseBody b = response.errorBody();
            System.out.println(b.string());
        } else fail();
    }

    private void waitForServerToStart() throws Exception {
        int code = -1;
        while (code != 200) {
            try {
                code = api.getStatus().execute().code();
                Thread.sleep(20);
            } catch (IOException e) {
                code = -1;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @After
    public void tearDown() throws Exception {
        springThread.stop();
        springThread.join();
    }
}
