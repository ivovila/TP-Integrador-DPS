package ar.edu.itba.certiflow.certification.model;

import java.time.LocalDate;

public record CertificationCondition(String findingId, String criterionCode, String description, LocalDate dueDate) { }
