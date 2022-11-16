package org.eclipse.openvsx.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.openvsx.entities.AzureDownloadCountProcessedItem;
import org.eclipse.openvsx.repositories.AzureDownloadCountProcessedItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
    "ovsx.elasticsearch.enabled=false", //
    "spring.datasource.username=postgres", "spring.datasource.password=postgres",})
@ActiveProfiles("test")
public class AzureDownloadCountProcessedItemRepositoryTest {

    @Autowired
    private AzureDownloadCountProcessedItemRepository underTest;

    @Test
    public void canFindAllSucceededAzureDownloadCountProcessedItemsByNameInGivenNames(){
        var dc = new AzureDownloadCountProcessedItem();
        String name = "forTest";
        dc.setId(new Random().nextLong());
        dc.setName(name);
        dc.setProcessedOn(LocalDateTime.now());
        dc.setExecutionTime(300);
        dc.setSuccess(true);

        underTest.save(dc);

        List<String> names = new ArrayList<String>();
        names.add(name);

        List<String> actual = underTest.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(names);
        assertThat(actual).isNotEmpty();

        underTest.delete(dc);


    }
    
}
