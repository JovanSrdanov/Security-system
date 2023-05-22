import React, {useEffect, useState} from "react";
import interceptor from "../../interceptor/interceptor";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  styled,
  Table,
  TableBody,
  TableCell,
  tableCellClasses,
  TableContainer,
  TableRow,
} from "@mui/material";
import {Flex} from "reflexbox";

const StyledTableCell = styled(TableCell)(({theme}) => ({
    [`&.${tableCellClasses.head}`]: {
        backgroundColor: theme.palette.common.black,
        color: theme.palette.common.white,
    },
    [`&.${tableCellClasses.body}`]: {
        fontSize: 14,
    },
}));

const StyledTableRow = styled(TableRow)(({theme}) => ({
    "&:nth-of-type(odd)": {
        backgroundColor: theme.palette.action.focusOpacity,
    },
}));

function EmployeesInfo(props) {
    const [allEmployees, setAllEmployees] = useState(null);
    const [selectedEmployee, setSelectedEmployee] = useState(null);

    const [viewSkillsDialog, setViewSkillsDialog] = useState(false);
    const [skills, setSkills] = useState(null);


    const getAllEmployees = () => {
        interceptor
            .get("employee")
            .then((res) => {

                setAllEmployees(res.data);
            })
            .catch((err) => {
                console.log(err);
            });
    };

    useEffect(() => {
        getAllEmployees();
    }, []);

    const handleCloseSkillsDialog = () => {
        setViewSkillsDialog(false)
        setSkills(null);
        setSelectedEmployee(null);

    };
    const handleViewProjects = (item) => {

        interceptor.get("sw-engineer/skill/" + item.employeeId).then((res) => {
            setSkills(res.data)
            console.log(res.data)
        }).catch((err) => {
            console.log(err)
        })


        setSelectedEmployee(item)
        setViewSkillsDialog(true)

    };


    return (
        <>

            <Dialog onClose={handleCloseSkillsDialog} open={viewSkillsDialog}>
                <DialogTitle>Skills:</DialogTitle>
                <DialogContent>
                    {skills != null && skills.length > 0 && (
                        <TableContainer component={Paper}
                                        sx={{maxHeight: 500, height: 500, overflowY: 'scroll'}}>
                            <Table>
                                <TableBody>
                                    {skills.map((item) => (
                                        <React.Fragment key={`${item.id}-row`}>
                                            <StyledTableRow>
                                                <StyledTableCell>
                                                    <Box m={1} sx={{
                                                        width: 150,
                                                        height: 50,
                                                        overflowX: 'auto',
                                                        overflowy: 'auto'
                                                    }}>
                                                        <li>Skill name: {item.name}</li>
                                                        <li>Level: {item.level}</li>
                                                    </Box>
                                                </StyledTableCell>
                                               
                                            </StyledTableRow>
                                        </React.Fragment>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </DialogContent>
                <DialogActions>
                    <Flex flexDirection="row" justifyContent="center" alignItems="center">
                        <Button onClick={handleCloseSkillsDialog}
                                variant="contained"
                        >
                            Close
                        </Button>
                    </Flex>
                </DialogActions>
            </Dialog>

            <div className="wrapper">
                {allEmployees != null && allEmployees.length > 0 && (
                    <TableContainer
                        component={Paper}
                        sx={{maxHeight: 500, height: 500, overflowY: "scroll"}}
                    >
                        <Table>
                            <TableBody>
                                {allEmployees.map((item) => (
                                    <React.Fragment key={`${item.employeeId}-row`}>
                                        <StyledTableRow>
                                            <StyledTableCell>
                                                <Box
                                                    m={1}
                                                    sx={{
                                                        overflowX: "auto",
                                                        width: 300,
                                                        height: 100,
                                                        overflowy: "auto",
                                                    }}
                                                >
                                                    <li>Email: {item.mail}</li>
                                                    <li>
                                                        Role:{" "}
                                                        {item.role.replace(/^ROLE_/, "").replace(/_/g, " ")}
                                                    </li>
                                                </Box>
                                            </StyledTableCell>
                                            <StyledTableCell>
                                                <Box
                                                    m={1}
                                                    sx={{
                                                        overflowX: "auto",
                                                        width: 300,
                                                        height: 100,
                                                        overflowy: "auto",
                                                    }}
                                                >
                                                    <li>Name: {item.name}</li>
                                                    <li>Surname: {item.surname}</li>
                                                    <li>Phone number: {item.phoneNumber}</li>
                                                    <li>Profession: {item.profession}</li>
                                                </Box>
                                            </StyledTableCell>
                                            <StyledTableCell>
                                                <Box
                                                    m={1}
                                                    sx={{
                                                        overflowX: "auto",
                                                        width: 300,
                                                        height: 100,
                                                        overflowy: "auto",
                                                    }}
                                                >
                                                    <li>Address:</li>
                                                    <li>
                                                        {item.address.city}, {item.address.country}
                                                    </li>
                                                    <li>
                                                        {item.address.street}, {item.address.streetNumber}
                                                    </li>
                                                </Box>
                                            </StyledTableCell>
                                            <StyledTableCell>
                                                <Box m={1}>
                                                    {item.role !== "HR manager" && (
                                                        <Box m={1}>
                                                            <Button fullWidth variant="contained" color="info">
                                                                View projects
                                                            </Button>
                                                        </Box>
                                                    )}
                                                    {item.role === "Software engineer" && (
                                                        <>
                                                            <Box m={1}>
                                                                <Button fullWidth variant="contained" color="primary"
                                                                        onClick={() => {
                                                                            handleViewProjects(item)
                                                                        }}
                                                                >
                                                                    View Skills
                                                                </Button>
                                                            </Box>
                                                            <Box m={1}>
                                                                <Button fullWidth variant="contained" color="success">
                                                                    Download cv TBA
                                                                </Button>
                                                            </Box>
                                                        </>
                                                    )}
                                                </Box>
                                            </StyledTableCell>
                                        </StyledTableRow>
                                    </React.Fragment>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </div>
        </>
    );
}

export default EmployeesInfo;
