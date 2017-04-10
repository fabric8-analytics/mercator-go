Feature: Test of Ruby handler

  Scenario: Succesfuly test gemspec file
    Given We have mercator installed
    When Scanning the gemspec file
    Then We have correct output
