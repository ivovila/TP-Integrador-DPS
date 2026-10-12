package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAssetRepository extends JpaRepository<AssetEntity, String> {
}
