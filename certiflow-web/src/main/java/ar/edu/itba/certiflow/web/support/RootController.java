package ar.edu.itba.certiflow.web.support;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RootController {

    @GetMapping
    public RepresentationModel<?> root() {
        return new RepresentationModel<>().add(
                Link.of("/api/v1").withSelfRel(),
                Link.of("/api/v1/assets").withRel("assets"),
                Link.of("/api/v1/schemas/{assetType}").withRel("schemas"),
                Link.of("/api/v1/inspections").withRel("inspections"),
                Link.of("/api/v1/findings").withRel("findings"),
                Link.of("/api/v1/certificates").withRel("certificates"));
    }
}
