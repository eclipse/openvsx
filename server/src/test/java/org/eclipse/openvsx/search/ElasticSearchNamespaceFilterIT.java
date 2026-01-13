@Test
void namespaceFilterMatchesOnlyExactNamespace() {

    Extension vscode = new Extension();
    vscode.setNamespace("vscode");
    vscode.setName("python");
    repositoryService.save(vscode);

    Extension msVscode = new Extension();
    msVscode.setNamespace("ms-vscode");
    msVscode.setName("cpptools");
    repositoryService.save(msVscode);

    // Rebuild the index so both extensions are searchable
    elasticSearchService.updateSearchIndex(true);

    // Search using an exact namespace filter
    Options options = new Options(
            null,
            "vscode",
            null,
            null,
            null,
            0,
            10,
            "desc",
            SortBy.RELEVANCE
    );

    SearchResult result = elasticSearchService.search(options);

    assertThat(result.extensions())
            .extracting(ExtensionSearch::getNamespace)
            .contains("vscode")
            .doesNotContain("ms-vscode");
}
