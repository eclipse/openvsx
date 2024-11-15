import { Reporter, TestResult } from "@playwright/test/reporter";
import fs from "fs";
import axios from "axios";

type PrometheusReporterOptions = {
  outputFile: string;
};

class PrometheusReporter implements Reporter {
  private pushGatewayUrl: string;
  private username: string;
  private password: string;
  private outputFile: string;
  private totalTests: number;
  private passedTests: number;
  private failedTests: number;
  private skippedTests: number;
  private startTime: number;
  private duration: number;

  constructor(options: PrometheusReporterOptions) {
    this.pushGatewayUrl = process.env.PROMETHEUS_PUSHGATEWAY_URL || "";
    this.username = process.env.PROMETHEUS_USERNAME || "";
    this.password = process.env.PROMETHEUS_PASSWORD || "";
    this.outputFile = options.outputFile;
    this.totalTests = 0;
    this.passedTests = 0;
    this.failedTests = 0;
    this.skippedTests = 0;
    this.startTime = Date.now();
  }

  onTestEnd(_: unknown, result: TestResult) {
    this.totalTests++;
    if (result.status === "passed") this.passedTests++;
    if (result.status === "failed") this.failedTests++;
    if (result.status === "skipped") this.skippedTests++;
  }

  async onEnd() {
    this.duration = Date.now() - this.startTime;
    const content = this.buildMetricsString();
    fs.writeFileSync(this.outputFile, content);
    this.pushMetricsToPrometheus(content);
  }

  private pushMetricsToPrometheus(content: string) {
    if (!this.pushGatewayUrl) return;
    axios.post(this.pushGatewayUrl, content, {
      headers: {
        "Content-Type": "text/plain",
      },
      auth: {
        username: this.username,
        password: this.password,
      },
    });
  }

  private buildMetricsString() {
    return `# HELP playwright_test_total The total number of tests
# TYPE playwright_test_total counter
playwright_test_total ${this.totalTests}
# HELP playwright_test_passed The number of passed tests
# TYPE playwright_test_passed counter
playwright_test_passed ${this.passedTests}
# HELP playwright_test_failed The number of failed tests
# TYPE playwright_test_failed counter
playwright_test_failed ${this.failedTests}
# HELP playwright_test_skipped The number of skipped tests
# TYPE playwright_test_skipped counter
playwright_test_skipped ${this.skippedTests}
# HELP playwright_test_duration The duration of the test run
# TYPE playwright_test_duration gauge
playwright_test_duration ${this.duration}
`;
  }
}

export default PrometheusReporter;
