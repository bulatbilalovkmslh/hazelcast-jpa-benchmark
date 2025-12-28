package org.example.controller;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.dto.*;
import org.example.entity.Customer;
import org.example.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class BenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkController.class);

    private final CustomerRepository customerRepository;
    private final EntityManagerFactory emf;
    private final EntityManager em;
    private final HazelcastInstance hazelcastInstance;
    private final RestTemplate restTemplate;

    @Value("${server.port}")
    private int serverPort;

    @Value("${app.node-id:unknown}")
    private String nodeId;

    public BenchmarkController(CustomerRepository customerRepository,
                               EntityManagerFactory emf,
                               EntityManager em,
                               HazelcastInstance hazelcastInstance,
                               RestTemplate restTemplate) {
        this.customerRepository = customerRepository;
        this.emf = emf;
        this.em = em;
        this.hazelcastInstance = hazelcastInstance;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/benchmark")
    public ResponseEntity<BenchmarkResult> benchmark() {
        ensureCustomer1Exists();

        int localReads = 5_000;

        String otherBaseUrl = (serverPort == 8081)
                ? "http://localhost:8082"
                : "http://localhost:8081";

        ModeResult noCacheLocal = runLocalReads(Mode.NO_CACHE, localReads);
        ModeResult noCacheRemote = callRemoteLocalBenchmark(otherBaseUrl, Mode.NO_CACHE, localReads);

        ModeResult l2Local = runLocalReads(Mode.L2_CACHE, localReads);
        ModeResult l2Remote = callRemoteLocalBenchmark(otherBaseUrl, Mode.L2_CACHE, localReads);

        ModeResult distLocal = runLocalReads(Mode.DISTRIBUTED_MAP, localReads);
        ModeResult distRemote = callRemoteLocalBenchmark(otherBaseUrl, Mode.DISTRIBUTED_MAP, localReads);

        BenchmarkResult result = new BenchmarkResult(
                noCacheLocal.elapsedMs() + noCacheRemote.elapsedMs(),
                l2Local.elapsedMs() + l2Remote.elapsedMs(),
                distLocal.elapsedMs() + distRemote.elapsedMs(),
                new TtlEffect(distLocal.hits() + distRemote.hits(), distLocal.misses() + distRemote.misses())
        );

        log.info("[{}] benchmark result: {}", nodeId, result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/benchmark/local")
    public ResponseEntity<ModeResult> benchmarkLocal(
            @RequestParam("mode") Mode mode,
            @RequestParam(value = "reads", defaultValue = "5000") int reads
    ) {
        ensureCustomer1Exists();
        return ResponseEntity.ok(runLocalReads(mode, reads));
    }

    private ModeResult callRemoteLocalBenchmark(String baseUrl, Mode mode, int reads) {
        String url = baseUrl + "/benchmark/local?mode=" + mode.name() + "&reads=" + reads;
        ModeResult remote = restTemplate.getForObject(url, ModeResult.class);
        if (remote == null) {
            throw new IllegalStateException("Remote benchmark result is null from " + url);
        }
        return remote;
    }

    private void ensureCustomer1Exists() {
        if (customerRepository.existsById(1L)) {
            return;
        }
        customerRepository.save(new Customer(1L, "Customer-1", "customer1@example.com"));
        log.info("[{}] created Customer(ID=1) for benchmark", nodeId);
    }

    private ModeResult runLocalReads(Mode mode, int reads) {
        long startNs = System.nanoTime();

        long hits = 0;
        long misses = 0;

        if (mode == Mode.NO_CACHE) {
            for (int i = 0; i < reads; i++) {
                em.clear();
                emf.getCache().evict(Customer.class, 1L);

                customerRepository.findById(1L).orElseThrow();
            }
        } else if (mode == Mode.L2_CACHE) {
            emf.getCache().evict(Customer.class, 1L);

            for (int i = 0; i < reads; i++) {
                em.clear();

                boolean inL2Before = emf.getCache().contains(Customer.class, 1L);
                if (inL2Before) hits++;
                else misses++;

                customerRepository.findById(1L).orElseThrow();
            }
        } else if (mode == Mode.DISTRIBUTED_MAP) {
            IMap<Long, CustomerDto> map = hazelcastInstance.getMap("customer-cache");
            map.delete(1L);

            int halfway = reads / 2;

            for (int i = 0; i < reads; i++) {
                em.clear();

                CustomerDto cached = map.get(1L);
                if (cached != null) {
                    hits++;
                } else {
                    misses++;

                    Customer fresh = em.find(
                            Customer.class,
                            1L,
                            Map.of(
                                    "jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS,
                                    "jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH
                            )
                    );
                    if (fresh == null) throw new IllegalStateException("Customer(ID=1) not found");

                    map.put(
                            1L,
                            new CustomerDto(fresh.getId(), fresh.getName(), fresh.getEmail()),
                            1,
                            TimeUnit.SECONDS
                    );
                }

                if (i == halfway) {
                    sleepSilently(Duration.ofMillis(1100));
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported mode: " + mode);
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        ModeResult result = new ModeResult(mode, elapsedMs, hits, misses, nodeId);
        log.info("[{}] local benchmark: {}", nodeId, result);
        return result;
    }

    private static void sleepSilently(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
