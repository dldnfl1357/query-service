package codegen

import codegen.ScreenSpec.Field
import codegen.ScreenSpec.Section

class ScreenGenerator {

    final ScreenSpec spec
    final File projectDir

    ScreenGenerator(ScreenSpec spec, File projectDir) {
        this.spec = spec
        this.projectDir = projectDir
    }

    void generate() {
        def base = packageDir()
        write(new File(base, "entity/${spec.screenPascal()}View.java"), renderEntity())
        write(new File(base, "repository/${spec.screenPascal()}ViewRepository.java"), renderRepository())
        write(new File(base, "service/${spec.screenPascal()}WriteService.java"), renderWriteService())
        write(new File(base, "service/${spec.screenPascal()}QueryService.java"), renderQueryService())
        write(new File(base, "api/${spec.screenPascal()}Response.java"), renderResponse())
        write(new File(base, "api/${spec.screenPascal()}Controller.java"), renderController())
        write(new File(base, "handler/${spec.screenPascal()}SqsHandlers.java"), renderHandlers())
        spec.sections.each { section ->
            write(new File(base, "event/${section.namePascal()}ChangedEvent.java"), renderEvent(section))
        }
        write(new File(projectDir, "src/main/resources/db/migration/${nextMigrationName()}"), renderMigration())
    }

    private File packageDir() {
        def relative = spec.basePackage.replace('.', '/')
        new File(projectDir, "src/main/java/${relative}/screens/${spec.screen}")
    }

    private String nextMigrationName() {
        def dir = new File(projectDir, "src/main/resources/db/migration")
        int next = 1
        if (dir.exists()) {
            dir.listFiles({ File f -> f.name =~ /^V(\d+)__/ } as FileFilter).each {
                def m = it.name =~ /^V(\d+)__/
                if (m) next = Math.max(next, (m[0][1] as Integer) + 1)
            }
        }
        "V${next}__init_${spec.screenSnake()}_view.sql"
    }

    private static void write(File file, String content) {
        file.parentFile.mkdirs()
        file.text = content
        println "wrote ${file}"
    }

    // ------ entity ------
    String renderEntity() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.entity"
        def imports = new LinkedHashSet<String>()
        imports << 'jakarta.persistence.Column'
        imports << 'jakarta.persistence.Entity'
        imports << 'jakarta.persistence.EntityListeners'
        imports << 'jakarta.persistence.Id'
        imports << 'jakarta.persistence.Index'
        imports << 'jakarta.persistence.Table'
        imports << 'lombok.AccessLevel'
        imports << 'lombok.Getter'
        imports << 'lombok.NoArgsConstructor'
        imports << 'lombok.Setter'
        imports << 'org.springframework.data.annotation.CreatedDate'
        imports << 'org.springframework.data.annotation.LastModifiedDate'
        imports << 'org.springframework.data.jpa.domain.support.AuditingEntityListener'
        imports << 'java.time.Instant'
        spec.sections*.fields*.each { Field f ->
            if (f.javaImport()) imports << f.javaImport()
        }

        def indexLines = spec.indexes.collect { cols ->
            def colNames = cols.collect { ScreenSpec.toSnake(it) }.join(',')
            def name = "idx_${spec.screenSnake()}_" + cols.collect { ScreenSpec.toSnake(it) }.join('_')
            "        @Index(name = \"${name}\", columnList = \"${colNames}\")"
        }

        def fieldDecls = []
        spec.sections.each { section ->
            section.fields.each { f ->
                fieldDecls << "    ${f.jpaColumnAnnotation()}\n    private ${f.type} ${f.name};"
            }
            fieldDecls << "    @Column(name = \"${section.sectionUpdatedAtColumn()}\")\n    private Instant ${section.sectionUpdatedAtField()};"
        }

        def importBlock = imports.sort().collect { "import ${it};" }.join('\n')
        def tableAnnotation = indexLines.isEmpty()
                ? "@Table(name = \"${spec.table}\")"
                : "@Table(\n        name = \"${spec.table}\",\n        indexes = {\n${indexLines.join(',\n')}\n        }\n)"

        """\
package ${pkg};

${importBlock}

@Entity
${tableAnnotation}
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ${spec.screenPascal()}View {

    @Id
    @Column(name = "${spec.primaryKeyColumn}")
    private ${spec.primaryKeyType} ${spec.primaryKeyField};

${fieldDecls.join('\n\n')}

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public static ${spec.screenPascal()}View empty(${spec.primaryKeyType} ${spec.primaryKeyField}) {
        ${spec.screenPascal()}View v = new ${spec.screenPascal()}View();
        v.${spec.primaryKeyField} = ${spec.primaryKeyField};
        return v;
    }
}
"""
    }

    // ------ repository ------
    String renderRepository() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.repository"
        def entityPkg = "${spec.basePackage}.screens.${spec.screen}.entity.${spec.screenPascal()}View"
        def methods = []
        spec.sections.findAll { !it.isRoot }.each { section ->
            def params = section.fields.collect { f -> "@Param(\"${f.name}\") ${f.type} ${f.name}" }
            params << "@Param(\"updatedAt\") Instant updatedAt"
            def setClauses = section.fields.collect { f -> "                   v.${f.name} = :${f.name}" }
            setClauses << "                   v.${section.sectionUpdatedAtField()} = :updatedAt"
            def methodName = "update${section.namePascal()}Section"
            methods << """\
    @Modifying
    @Query(\"\"\"
            update ${spec.screenPascal()}View v
               set ${setClauses.join(',\n').trim()}
             where v.${section.matchByField} = :${section.matchByField}
               and (v.${section.sectionUpdatedAtField()} is null or v.${section.sectionUpdatedAtField()} < :updatedAt)
            \"\"\")
    int ${methodName}(
            @Param("${section.matchByField}") ${matchByType(section)} ${section.matchByField},
${params.collect { "            ${it}" }.join(',\n')}
    );"""
        }

        """\
package ${pkg};

import ${entityPkg};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface ${spec.screenPascal()}ViewRepository extends JpaRepository<${spec.screenPascal()}View, ${spec.primaryKeyType}> {

${methods.join('\n\n')}
}
"""
    }

    private String matchByType(Section section) {
        if (section.matchByField == spec.primaryKeyField) return spec.primaryKeyType
        // assume FK to root; fall back to Long
        'Long'
    }

    // ------ event records ------
    String renderEvent(Section section) {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.event"
        def fieldsForEvent = []
        if (section.isRoot) {
            fieldsForEvent << "${spec.primaryKeyType} ${spec.primaryKeyField}"
            section.fields.each { f -> fieldsForEvent << "${f.type} ${f.name}" }
            // also include FK fields for non-root sections (matchBy != PK)
            spec.sections.findAll { !it.isRoot && it.matchByField != spec.primaryKeyField }.each {
                fieldsForEvent << "Long ${it.matchByField}"
            }
        } else {
            fieldsForEvent << "${matchByType(section)} ${section.matchByField}"
            section.fields.each { f -> fieldsForEvent << "${f.type} ${f.name}" }
        }

        def imports = new LinkedHashSet<String>()
        imports << 'java.time.Instant'
        section.fields.each { f -> if (f.javaImport()) imports << f.javaImport() }

        def importBlock = imports.sort().collect { "import ${it};" }.join('\n')
        def header = ['String eventId'] + fieldsForEvent + ['Instant occurredAt']

        """\
package ${pkg};

${importBlock}

public record ${section.namePascal()}ChangedEvent(
${header.collect { "        ${it}" }.join(',\n')}
) {
}
"""
    }

    // ------ handlers ------
    String renderHandlers() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.handler"
        def methods = spec.sections.collect { section ->
            """\
    @SqsListener("\${${section.queueProperty}}")
    public void on${section.namePascal()}Changed(${section.namePascal()}ChangedEvent event) {
        log.info("${section.name} event received eventId={}", event.eventId());
        writeService.apply${section.namePascal()}(event);
    }"""
        }.join('\n\n')

        def imports = ["${spec.basePackage}.screens.${spec.screen}.service.${spec.screenPascal()}WriteService"]
        spec.sections.each { s -> imports << "${spec.basePackage}.screens.${spec.screen}.event.${s.namePascal()}ChangedEvent" }
        imports << 'io.awspring.cloud.sqs.annotation.SqsListener'
        imports << 'lombok.RequiredArgsConstructor'
        imports << 'lombok.extern.slf4j.Slf4j'
        imports << 'org.springframework.stereotype.Component'

        """\
package ${pkg};

${imports.sort().collect { "import ${it};" }.join('\n')}

@Slf4j
@Component
@RequiredArgsConstructor
public class ${spec.screenPascal()}SqsHandlers {

    private final ${spec.screenPascal()}WriteService writeService;

${methods}
}
"""
    }

    // ------ write service ------
    String renderWriteService() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.service"
        def applyMethods = spec.sections.collect { section ->
            section.isRoot ? renderRootApply(section) : renderSectionApply(section)
        }.join('\n\n')

        def imports = new LinkedHashSet<String>()
        imports << "${spec.basePackage}.common.idempotency.IdempotencyService"
        imports << "${spec.basePackage}.screens.${spec.screen}.entity.${spec.screenPascal()}View"
        imports << "${spec.basePackage}.screens.${spec.screen}.repository.${spec.screenPascal()}ViewRepository"
        spec.sections.each { s ->
            imports << "${spec.basePackage}.screens.${spec.screen}.event.${s.namePascal()}ChangedEvent"
        }
        imports << 'lombok.RequiredArgsConstructor'
        imports << 'lombok.extern.slf4j.Slf4j'
        imports << 'org.springframework.stereotype.Service'
        imports << 'org.springframework.transaction.annotation.Transactional'

        """\
package ${pkg};

${imports.sort().collect { "import ${it};" }.join('\n')}

@Slf4j
@Service
@RequiredArgsConstructor
public class ${spec.screenPascal()}WriteService {

    private static final String CONSUMER = "${spec.screen}";

    private final ${spec.screenPascal()}ViewRepository repository;
    private final IdempotencyService idempotency;

${applyMethods}
}
"""
    }

    private String renderRootApply(Section root) {
        def setters = root.fields.collect { f -> "        view.set${ScreenSpec.capitalize(f.name)}(event.${f.name}());" }
        // also set FK fields from event
        spec.sections.findAll { !it.isRoot && it.matchByField != spec.primaryKeyField }.each { s ->
            setters << "        view.set${ScreenSpec.capitalize(s.matchByField)}(event.${s.matchByField}());"
        }

        """\
    @Transactional
    public void apply${root.namePascal()}(${root.namePascal()}ChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }

        ${spec.screenPascal()}View view = repository.findById(event.${spec.primaryKeyField}())
                .orElseGet(() -> ${spec.screenPascal()}View.empty(event.${spec.primaryKeyField}()));

        if (view.get${ScreenSpec.capitalize(root.sectionUpdatedAtField())}() != null
                && !event.occurredAt().isAfter(view.get${ScreenSpec.capitalize(root.sectionUpdatedAtField())}())) {
            log.debug("skip stale ${root.name} event ${spec.primaryKeyField}={} occurredAt={}", event.${spec.primaryKeyField}(), event.occurredAt());
            return;
        }

${setters.join('\n')}
        view.set${ScreenSpec.capitalize(root.sectionUpdatedAtField())}(event.occurredAt());
        repository.save(view);
    }"""
    }

    private String renderSectionApply(Section section) {
        def args = [
                "event.${section.matchByField}()",
        ] + section.fields.collect { f -> "event.${f.name}()" } + ["event.occurredAt()"]

        """\
    @Transactional
    public void apply${section.namePascal()}(${section.namePascal()}ChangedEvent event) {
        if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
            log.debug("duplicate event skipped consumer={} eventId={}", CONSUMER, event.eventId());
            return;
        }
        int updated = repository.update${section.namePascal()}Section(
                ${args.join(',\n                ')}
        );
        log.debug("${section.name} section updated ${section.matchByField}={} rows={}", event.${section.matchByField}(), updated);
    }"""
    }

    // ------ query service ------
    String renderQueryService() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.service"
        """\
package ${pkg};

import ${spec.basePackage}.screens.${spec.screen}.api.${spec.screenPascal()}Response;
import ${spec.basePackage}.screens.${spec.screen}.repository.${spec.screenPascal()}ViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ${spec.screenPascal()}QueryService {

    private final ${spec.screenPascal()}ViewRepository repository;

    public Page<${spec.screenPascal()}Response> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(${spec.screenPascal()}Response::from);
    }
}
"""
    }

    // ------ response ------
    String renderResponse() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.api"
        def imports = new LinkedHashSet<String>()
        imports << "${spec.basePackage}.screens.${spec.screen}.entity.${spec.screenPascal()}View"
        imports << 'java.time.Instant'
        spec.sections*.fields*.each { Field f ->
            if (f.javaImport()) imports << f.javaImport()
        }

        def header = ["${spec.primaryKeyType} ${spec.primaryKeyField}"]
        spec.sections.each { s -> s.fields.each { f -> header << "${f.type} ${f.name}" } }

        def fromAssignments = ["                v.get${ScreenSpec.capitalize(spec.primaryKeyField)}()"]
        spec.sections.each { s ->
            s.fields.each { f -> fromAssignments << "                v.get${ScreenSpec.capitalize(f.name)}()" }
        }

        """\
package ${pkg};

${imports.sort().collect { "import ${it};" }.join('\n')}

public record ${spec.screenPascal()}Response(
${header.collect { "        ${it}" }.join(',\n')}
) {

    public static ${spec.screenPascal()}Response from(${spec.screenPascal()}View v) {
        return new ${spec.screenPascal()}Response(
${fromAssignments.join(',\n')}
        );
    }
}
"""
    }

    // ------ controller ------
    String renderController() {
        def pkg = "${spec.basePackage}.screens.${spec.screen}.api"
        def pathSegment = ScreenSpec.toSnake(spec.screen).replace('_', '-')
        """\
package ${pkg};

import ${spec.basePackage}.screens.${spec.screen}.service.${spec.screenPascal()}QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${pathSegment}")
@RequiredArgsConstructor
public class ${spec.screenPascal()}Controller {

    private final ${spec.screenPascal()}QueryService queryService;

    @GetMapping
    public Page<${spec.screenPascal()}Response> list(@PageableDefault(size = 50) Pageable pageable) {
        return queryService.findAll(pageable);
    }
}
"""
    }

    // ------ migration ------
    String renderMigration() {
        def lines = []
        lines << "    ${spec.primaryKeyColumn.padRight(28)} ${spec.primaryKeyType == 'Long' ? 'bigint' : 'bigint'} not null,"
        spec.sections.each { section ->
            section.fields.each { f ->
                lines << "    ${f.column.padRight(28)} ${f.sqlType()},"
            }
            lines << "    ${section.sectionUpdatedAtColumn().padRight(28)} datetime(6),"
        }
        lines << "    ${'created_at'.padRight(28)} datetime(6),"
        lines << "    ${'updated_at'.padRight(28)} datetime(6),"
        lines << "    primary key (${spec.primaryKeyColumn})"

        def indexLines = spec.indexes.collect { cols ->
            def name = "idx_${spec.screenSnake()}_" + cols.collect { ScreenSpec.toSnake(it) }.join('_')
            "    key ${name} (${cols.collect { ScreenSpec.toSnake(it) }.join(', ')})"
        }
        if (!indexLines.isEmpty()) {
            lines[-1] = lines[-1] + ','
            lines.addAll(indexLines.withIndex().collect { line, i ->
                i == indexLines.size() - 1 ? line : "${line},"
            })
        }

        """\
create table if not exists ${spec.table}
(
${lines.join('\n')}
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_unicode_ci;
"""
    }
}
