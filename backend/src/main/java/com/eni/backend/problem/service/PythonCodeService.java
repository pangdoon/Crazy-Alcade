package com.eni.backend.problem.service;

import com.eni.backend.common.exception.CustomServerErrorException;
import com.eni.backend.problem.dto.response.CodeExecuteResponse;
import com.eni.backend.problem.entity.CodeStatus;
import com.eni.backend.problem.entity.Problem;
import com.eni.backend.problem.entity.Testcase;
import com.eni.backend.problem.repository.TestcaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static com.eni.backend.common.response.BaseResponseStatus.SERVER_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonCodeService {

    @Value("${code.base-path}")
    private String BASE_PATH;

    private final TestcaseRepository testcaseRepository;

    public Object execute(Problem problem, String code) throws IOException, InterruptedException {
        // 파일을 저장할 디렉토리 생성
        UUID uuid = UUID.randomUUID();
        String dirPath = createDirectory(uuid);

        // 컴파일 에러
        if (!compile(dirPath, code)) {
            log.info("컴파일 에러");
            return CodeStatus.COMPILE_ERROR.getStatus();
        }

        log.info("컴파일 성공");

        List<CodeExecuteResponse> responses = new ArrayList<>();
        List<Testcase> testcases = testcaseRepository.findAllByProblemIdAndIsHidden(problem.getId(), false);

        // 각 테스트케이스 별 실행 결과
        for (int i=0; i<testcases.size(); i++) {
            responses.add(judge(dirPath,i+1, testcases.get(i)));
        }

        // 파일 삭제
        deleteFolder(dirPath);

        return responses;
    }

    private boolean compile(String dirPath, String code) throws IOException, InterruptedException {
        boolean result = true;

        // 파이썬 파일 생성
        String codePath = createPythonFile(dirPath, code);

        // 컴파일
        ProcessBuilder pb = new ProcessBuilder("python3", "-m", "py_compile", codePath);
        Process process = pb.start();
        process.waitFor();

        // 실패
        if (process.exitValue() != 0) {
            // 파일 삭제
            deleteFolder(dirPath);
            // 결과값
            result = false;
        }

        process.destroyForcibly();
        return result;
    }

    private CodeExecuteResponse judge(String dirPath, Integer no, Testcase testcase) throws IOException, InterruptedException {
        // 테스트 케이스 input 생성
        String inputPath = createInputFile(dirPath, no, testcase.getInput());

        // 코드 실행
        ProcessBuilder pb = new ProcessBuilder("python3", dirPath + "Solution.py")
                .redirectInput(new File(inputPath)); // 테스트 케이스 input

        // 표준 에러를 표준 출력으로 redirect
//        pb.redirectErrorStream(true);

        // 채점 시작
        Process process = pb.start();
        process.waitFor();

        BufferedReader outputReader;
        StringBuilder result;
        String line;

        // 런타임 에러
        if (process.exitValue() != 0) {
            outputReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            result = new StringBuilder();
            while ((line = outputReader.readLine()) != null) {
                result.append(line).append(" ");
            }

            log.info("런타임 에러 {}", result);

            process.destroyForcibly();
            deleteFile(inputPath);

            return CodeExecuteResponse.of(no, CodeStatus.RUNTIME_ERROR.getStatus());
        }

        // 실행 결과
        outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        result = new StringBuilder();
        while ((line = outputReader.readLine()) != null) {
            result.append(line).append("\n");
        }

        log.info("실행 결과 {}", result);

        // 테스트케이스 output 생성
        String outputPath = createOutputFile(dirPath, no, testcase.getOutput());
        if (outputPath == null) {
            process.destroyForcibly();
            deleteFolder(dirPath);
            throw new CustomServerErrorException(SERVER_ERROR);
        }

        // String 변환
        StringBuilder output = new StringBuilder();
        try (Scanner sc = new Scanner(new File(outputPath))) {
            while (sc.hasNextLine()) {
                output.append(sc.nextLine()).append("\n");
            }
        } catch (Exception e) {
            process.destroyForcibly();
            deleteFolder(dirPath);
            throw new CustomServerErrorException(SERVER_ERROR);
        }

        // 파일 삭제
        deleteFile(inputPath);
        deleteFile(outputPath);
        // 프로세스 파괴
        process.destroyForcibly();

        // 실패
        if (!result.toString().equals(output.toString())) {
            return CodeExecuteResponse.of(no, CodeStatus.FAIL.getStatus());
        }

        // 성공
        return CodeExecuteResponse.of(no, CodeStatus.SUCCESS.getStatus());
    }

    private String createDirectory(UUID uuid) {
        String path = BASE_PATH + uuid + File.separator;
        File file = new File(path);

        if (file.exists()) {
            return path;
        }

        if (file.mkdir()) {
            log.info("폴더 생성 {}", path);
            return path;
        }

        throw new CustomServerErrorException(SERVER_ERROR);
    }

    private String createPythonFile(String dirPath, String content) throws IOException {
        // 파일 생성
        String path = dirPath + "Solution.py";
        File file = new File(path);

        // 파일 쓰기
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (Exception e) {
            deleteFolder(dirPath);
            throw new CustomServerErrorException(SERVER_ERROR);
        }

        log.info("코드 생성 {}", path);
        // 파일 경로 반환
        return path;
    }

    private String createInputFile(String dirPath, int no, String input) throws IOException {
        // 파일 생성
        String path = dirPath + no + "input.txt";
        File file = new File(path);

        // 파일 쓰기
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(input);
        } catch (Exception e) {
            deleteFolder(dirPath);
            throw new CustomServerErrorException(SERVER_ERROR);
        }

        log.info("input{} 생성 {}", no, path);
        // 파일 경로 반환
        return path;
    }

    private String createOutputFile(String dirPath, int no, String output) {
        // 파일 생성
        String path = dirPath + no + "output.txt";
        File file = new File(path);

        // 파일 쓰기
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(output);
        } catch (Exception e) {
            log.info("output{} 생성 실패", no);
            return null;
        }

        log.info("output{} 생성 {}", no, path);
        // 파일 경로 반환
        return path;
    }

    private void deleteFolder(String path) throws IOException {
        FileUtils.cleanDirectory(new File(path));
        deleteFile(path);
    }

    private void deleteFile(String path) {
        File file = new File(path);
        file.delete();
    }

}
