package ar.edu.itba.certiflow.web.certification;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificateApiTest extends ApiTest {

    private final Flows flows = new Flows(this);
    private String inspection;
    private String certificate;

    @BeforeEach
    void issueACertificate() throws Exception {
        flows.assetAndSchema();
        inspection = flows.closedInspection("7.00");
        certificate = flows.certificateFor(inspection);
    }

    @Test
    void anActiveCertificateOffersSuspensionAndRenewal() throws Exception {
        read(certificate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$._links.suspensions.href").exists())
                .andExpect(jsonPath("$._links.renewals.href").exists())
                .andExpect(jsonPath("$._links.document.href").exists());
    }

    @Test
    void suspendingLeavesItSuspendedWithoutAnotherSuspensionAndCannotBeRepeated() throws Exception {
        postAs("EMP-001", certificate + "/suspensions", "{\"reason\":\"Seal tampered\"}").andExpect(status().isCreated());

        read(certificate)
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$._links.suspensions").doesNotExist())
                .andExpect(jsonPath("$._links.renewals.href").exists());
        postAs("EMP-001", certificate + "/suspensions", "{\"reason\":\"Again\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("CertificateNotActiveException"));
    }

    @Test
    void renewingIssuesANewCertificateAndRetiresTheOldOne() throws Exception {
        String second = flows.closedInspection("7.10");

        postAs("EMP-006", certificate + "/renewals",
                "{\"inspectionId\":\"" + Flows.idOf(second) + "\",\"validUntil\":\"" + LocalDate.now().plusYears(2) + "\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/certificates/CERT-00002")));

        read(certificate)
                .andExpect(jsonPath("$.status").value("RENEWED"))
                .andExpect(jsonPath("$._links.suspensions").doesNotExist())
                .andExpect(jsonPath("$._links.renewals").doesNotExist());
    }

    @Test
    void issuingTwiceForTheSameAssetIs409() throws Exception {
        postAs("EMP-006", "/api/v1/certificates",
                "{\"inspectionId\":\"" + Flows.idOf(inspection) + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("AssetAlreadyCertifiedException"));
    }

    @Test
    void certificatesAreListedByAssetAndTheDocumentIsAvailable() throws Exception {
        read("/api/v1/certificates?asset=EXT-0042")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.certificates.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        read(certificate + "/document").andExpect(status().isOk()).andExpect(jsonPath("$.assetName").value("Lobby extinguisher"));
    }

    @Test
    void anUnknownCertificateIs404() throws Exception {
        read("/api/v1/certificates/CERT-99999").andExpect(status().isNotFound());
    }

    @Test
    void theDocumentUsesPlainValues() throws Exception {
        read(certificate + "/document")
                .andExpect(jsonPath("$.number").value("CERT-00001"))
                .andExpect(jsonPath("$.assetCode").value("EXT-0042"))
                .andExpect(jsonPath("$.validUntil").value(LocalDate.now().plusYears(1).toString()))
                .andExpect(jsonPath("$.issuedBy.id").value("EMP-006"));
    }

    @Test
    void aSuspensionIsAResourceOfItsOwn() throws Exception {
        String location = postAs("EMP-001", certificate + "/suspensions", "{\"reason\":\"Seal tampered\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(certificate + "/suspensions/1")))
                .andReturn().getResponse().getHeader("Location");

        read(Flows.path(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Seal tampered"))
                .andExpect(jsonPath("$._links.certificate.href").exists());
    }
}
