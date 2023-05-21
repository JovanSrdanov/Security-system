import React from 'react';
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
    TextField
} from "@mui/material";
import {Flex} from "reflexbox";
import interceptor from "../../interceptor/interceptor";

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
    '&:nth-of-type(odd)': {
        backgroundColor: theme.palette.action.focusOpacity,
    }
}));

function Skills(props) {
    const [mySkills, setMySkills] = React.useState([{id: ""}]);
    const [seniority, setSeniority] = React.useState({seniority: "Junior", dateOfEmployment: new Date()});

    const [showAddNewSkillDialog, setShowAddNewSkillDialog] = React.useState(false);
    const [skillName, setSkillName] = React.useState("");
    const [skillLevel, setSkillLevel] = React.useState(1);

    const handleCloseAddNewSkillsDialog = () => {
        setShowAddNewSkillDialog(false)
        setSkillName("")
        setSkillLevel(1)
    };


    const handleAddingSkill = () => {
        interceptor.post("sw-engineer/skill", {name: skillName, level: parseInt(skillLevel)}).then((res) => {
            console.log(res.data)

        }).catch((err) => {
            console.log(err)
        })


    };
    return (
        <>
            <Dialog onClose={handleCloseAddNewSkillsDialog} open={showAddNewSkillDialog}>
                <DialogTitle>Reason for rejection:</DialogTitle>
                <DialogContent>
                    <Box m={1}>
                        <TextField
                            label="Skill"
                            variant="filled"
                            value={skillName}
                            onChange={(event) => setSkillName(event.target.value)}
                        />
                    </Box>
                    <Box m={1}>
                        <TextField
                            variant="filled"
                            type="number"
                            label="Skill level"
                            InputProps={{inputProps: {min: 1, max: 5}}}
                            name="skillLevel"
                            value={skillLevel}
                            onChange={(event) => setSkillLevel(event.target.value)}
                        />
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Flex flexDirection="row" justifyContent="center" alignItems="center">
                        <Button onClick={handleCloseAddNewSkillsDialog}
                                variant="contained"

                        >
                            Close
                        </Button>

                        <Box m={1}>
                            <Button
                                disabled={skillName === ""}
                                variant="contained" color="error"
                                onClick={handleAddingSkill}

                            >Add skill</Button>
                        </Box>
                    </Flex>


                </DialogActions>
            </Dialog>


            <div className="wrapper">
                <Flex flexDirection="column" justifyContent="center" alignItems="center">
                    <Box m={1}>
                        Seniority: {seniority.seniority}
                    </Box>
                    <Box m={1}>
                        Date Of
                        Employment: {seniority.dateOfEmployment.toLocaleTimeString("en-GB")} {seniority.dateOfEmployment.toLocaleDateString("en-GB")}
                    </Box>
                </Flex>
                {mySkills != null && mySkills.length > 0 && (
                    <TableContainer component={Paper}
                                    sx={{maxHeight: 500, height: 500, overflowY: 'scroll'}}>
                        <Table>
                            <TableBody>
                                {mySkills.map((item) => (
                                    <React.Fragment key={`${item.id}-row`}>
                                        <StyledTableRow>
                                            <StyledTableCell>
                                                <Box m={1} sx={{
                                                    width: 150,
                                                    height: 50,
                                                    overflowX: 'auto',
                                                    overflowy: 'auto'
                                                }}>
                                                    <li>Skill:</li>
                                                    <li>Grade:</li>
                                                </Box>
                                            </StyledTableCell>
                                            <StyledTableCell>
                                                <Button fullWidth variant="outlined"
                                                        color="error"
                                                >Remove skill
                                                </Button>
                                            </StyledTableCell>
                                        </StyledTableRow>
                                    </React.Fragment>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
                <Button fullWidth variant="contained"
                        color="success"
                        onClick={() => {
                            setShowAddNewSkillDialog(true)
                        }}
                >Add skill
                </Button>

            </div>

        </>
    );
}

export default Skills;