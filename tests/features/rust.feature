Feature: Test of Rust handler

  Scenario: Succesfuly test Cargo.toml file
    Given We have mercator installed
    When Scanning the Cargo.toml file
    Then We have correct output
