Feature: Test of Python-Dist handler

  Scenario: Succesfuly test PKGINFO file
    Given We have mercator installed
    When Scanning the PKGINFO file
    Then We have correct output
