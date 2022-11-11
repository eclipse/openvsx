package org.eclipse.openvsx.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.ExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.junit.jupiter.api.Test;

@DataJpaTest
public class ExtensionRepositoryTest {

    @Autowired
    private ExtensionRepository underTest;

    @Test
    public void canGetMaxDownloadCount(){
        var ext1 = new Extension();
        ext1.setId(new Random().nextLong());
        ext1.setDownloadCount(0);

        var ext2 = new Extension();
        ext2.setId(new Random().nextLong());
        ext2.setDownloadCount(10);

        underTest.save(ext1);
        underTest.save(ext2);

        int actual = underTest.getMaxDownloadCount();
        assertEquals(10, actual);

        underTest.delete(ext1);
        underTest.delete(ext2);

    }

    
}
