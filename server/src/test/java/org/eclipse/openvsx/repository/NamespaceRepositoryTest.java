package org.eclipse.openvsx.repository;

import java.util.Random;

import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.NamespaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.util.Streamable;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
    "ovsx.elasticsearch.enabled=false", //
    "spring.datasource.username=postgres", "spring.datasource.password=postgres",})
@ActiveProfiles("test")
public class NamespaceRepositoryTest {

    @Autowired
    private NamespaceRepository underTest;

    @Test
    public void canFindOrphans(){
        var exampleOrphan = new Namespace();
        Long id = new Random().nextLong();
        String name = "testnamespace";

        exampleOrphan.setId(id);
        exampleOrphan.setPublicId("testPublicId");
        exampleOrphan.setName(name);

        underTest.save(exampleOrphan);
        
        Streamable<Namespace> actual = underTest.findOrphans();
        assertThat(actual).isNotEmpty();

        underTest.delete(exampleOrphan);
    }
    
}
