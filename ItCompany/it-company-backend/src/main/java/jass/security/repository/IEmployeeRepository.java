package jass.security.repository;

import jass.security.dto.employee.EmployeeInfoDto;
import jass.security.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IEmployeeRepository extends JpaRepository<Employee, UUID> {
    @Query("select new jass.security.dto.employee.EmployeeInfoDto(engineer.id, account.email,  'Software engineer'," +
            " engineer.address.country, engineer.address.city," +
            "engineer.address.street, engineer.address.streetNumber, engineer.name, engineer.phoneNumber," +
            " engineer.profession, engineer.surname, account.isBlocked) " +
            "from SoftwareEngineer engineer left join Account account " +
            "on engineer.id = account.employeeId " +
            "where account.status = 1 " +

            " union " +

            "select new jass.security.dto.employee.EmployeeInfoDto(prManager.id, account.email,  'Project manager'," +
            " prManager.address.country, prManager.address.city," +
            "prManager.address.street, prManager.address.streetNumber, prManager.name, prManager.phoneNumber," +
            " prManager.profession, prManager.surname, account.isBlocked) " +
            "from ProjectManager prManager left join Account account " +
            "on prManager.id = account.employeeId " +
            "where account.status = 1 " +


            "union " +

            "select new jass.security.dto.employee.EmployeeInfoDto(hrManager.id, account.email,  'HR manager'," +
            " hrManager.address.country, hrManager.address.city," +
            "hrManager.address.street, hrManager.address.streetNumber, hrManager.name, hrManager.phoneNumber," +
            " hrManager.profession, hrManager.surname, account.isBlocked) " +
            "from HrManager hrManager left join Account account " +
            "on hrManager.id = account.employeeId " +
            "where account.status = 1"
    )
    List<EmployeeInfoDto> getAll();

    @Query("select engineer.id " +
            "from SoftwareEngineer engineer left join " +
            "SwEngineerProjectStats stats " +
            "on engineer.id = stats.id.swEngineerId " +
            "where stats.id.projectId = :projectId and stats.workingPeriod.endDate is null ")
    List<UUID> getAllEmployedEngineerOnProjectId(UUID projectId);

    @Query("select manager.id " +
            "from ProjectManager manager left join " +
            "PrManagerProjectStats stats " +
            "on manager.id = stats.id.prManagerId " +
            "where stats.id.projectId = :projectId and stats.workingPeriod.endDate is null")
    List<UUID> getAllEmployedPrManagerOnProjectId(UUID projectId);


    @Query("select new jass.security.dto.employee.EmployeeInfoDto(engineer.id, account.email,  'Software engineer'," +
            " engineer.address.country, engineer.address.city," +
            "engineer.address.street, engineer.address.streetNumber, engineer.name, engineer.phoneNumber," +
            " engineer.profession, engineer.surname, account.isBlocked) " +
            "from SoftwareEngineer engineer left join Account account " +
            "on engineer.id = account.employeeId " +
            "where engineer.id not in :employeeIds and account.status = 1 ")
    List<EmployeeInfoDto> getOppositeEmployeeEngineerGroup(@Param("employeeIds") List<UUID> employeeIds);


    @Query("select new jass.security.dto.employee.EmployeeInfoDto(prManager.id, account.email,  'Project manager'," +
            " prManager.address.country, prManager.address.city," +
            "prManager.address.street, prManager.address.streetNumber, prManager.name, prManager.phoneNumber," +
            " prManager.profession, prManager.surname, account.isBlocked) " +
            "from ProjectManager prManager left join Account account " +
            "on prManager.id = account.employeeId " +
            "where prManager.id not in :employeeIds and account.status = 1")
    List<EmployeeInfoDto> getOppositeEmployeePrManagerGroup(@Param("employeeIds") List<UUID> employeeIds);
}
