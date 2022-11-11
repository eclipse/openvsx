package org.eclipse.openvsx.repository;

import java.util.Random;

import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.NamespaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.util.Streamable;
import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
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
