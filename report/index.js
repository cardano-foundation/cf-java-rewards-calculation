window.onload = () => {
    const actualTreasury = {
        name: 'Actual Treasury',
        x: [],
        y: [],
        mode: 'lines+markers',
        connectgaps: true,
        line: {
            color: '#1EC198'
        }
    }

    const calculatedTreasury = {
        name: 'Calculated Treasury',
        x: [],
        y: [],
        mode: 'lines+markers',
        connectgaps: true,
        line: {
            color: '#5C8DFF'
        } 
    }

    const difference = {
        name: 'Difference',
        x: [],
        y: [],
        mode: 'lines+markers',
        connectgaps: true,
        line: {
            color: '#1EC198'
        } 
    }

    const relativeDifference = {
        name: 'Difference in %',
        x: [],
        y: [],
        mode: 'lines+markers',
        connectgaps: true,
        line: {
            color: '#1EC198'
        } 
    }

    const lovelaceToAda = (lovelace) => {
        return Math.round(lovelace / 1000000);
    }

    const treasuryCalculationTable = document.getElementById('treasury-calculation-table');

    for (const epoch of Object.keys(treasuryCalculationResult)) {
        actualTreasury.x.push(epoch);
        actualTreasury.y.push(lovelaceToAda(treasuryCalculationResult[epoch].actualTreasury));
        calculatedTreasury.x.push(epoch);
        calculatedTreasury.y.push(lovelaceToAda(treasuryCalculationResult[epoch].calculatedTreasury));
        difference.x.push(epoch);
        difference.y.push(lovelaceToAda(
            treasuryCalculationResult[epoch].actualTreasury - treasuryCalculationResult[epoch].calculatedTreasury
        ));

        relativeDifference.x.push(epoch);
        relativeDifference.y.push(
            Math.round(
                (Math.abs(treasuryCalculationResult[epoch].actualTreasury - treasuryCalculationResult[epoch].calculatedTreasury) /
                Math.abs((treasuryCalculationResult[epoch].actualTreasury + treasuryCalculationResult[epoch].calculatedTreasury) / 2) * 100) * 1000) / 1000
        );

        const differenceAbsolut = lovelaceToAda(treasuryCalculationResult[epoch].actualTreasury - treasuryCalculationResult[epoch].calculatedTreasury)
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${epoch}</td>
            <td>${lovelaceToAda(treasuryCalculationResult[epoch].actualTreasury)}₳</td>
            <td>${lovelaceToAda(treasuryCalculationResult[epoch].calculatedTreasury)}₳</td>
            <td>${differenceAbsolut}₳</td>
            <td>${Math.round(
                (Math.abs(treasuryCalculationResult[epoch].actualTreasury - treasuryCalculationResult[epoch].calculatedTreasury) /
                Math.abs((treasuryCalculationResult[epoch].actualTreasury + treasuryCalculationResult[epoch].calculatedTreasury) / 2) * 100) * 1000) / 1000
            }%</td>
            <td>${Math.abs(differenceAbsolut) > 20000 ? 'Significant Difference' : ''}</td>
        `;

        if (Math.abs(differenceAbsolut) > 20000) {
            row.classList.add('huge-difference');
        }

        treasuryCalculationTable.appendChild(row);
    }

    const layoutAda = {
        title: 'Treasury Calculation & Actual Values - Difference in ADA',
        showlegend: false,
        xaxis: {
            title: 'Epoch',
            showgrid: false,
            zeroline: false
        },
        yaxis: {
            title: '₳',
            showline: false
        }
    };

    const layoutPercentage = {
        title: 'Treasury Calculation & Actual Values - Difference in ADA',
        showlegend: false,
        xaxis: {
            title: 'Epoch',
            showgrid: false,
            zeroline: false
        },
        yaxis: {
            title: '%',
            showline: false
        }
    };

    Plotly.newPlot('calculated-vs-actual-plot', [actualTreasury, calculatedTreasury], layoutAda);
    Plotly.newPlot('difference-plot', [difference], layoutAda);
    Plotly.newPlot('difference-percentage-plot', [relativeDifference], layoutPercentage);

    const epochInfo = document.getElementById('epoch-info');
    const epochs = Object.keys(treasuryCalculationResult).map((epoch) => Number(epoch)); 
    const epochStart = Math.min(...epochs);
    const epochEnd = Math.max(...epochs);
    epochInfo.innerHTML = `Epoch ${epochStart} - ${epochEnd}`;

    const fillCard = (cardId, title, value, subtile) => {
        const card = document.getElementById(cardId);
        card.innerHTML = `
            <div class="card-content">
                <h5 class="title">${title}</h5>
                <h2 class="value">${value}</h2>
                <p class="subtitle">${subtile}</p>
            </div>
        `;
    };

    const highestDifference = Math.max(...relativeDifference.y);
    const highestDifferenceEpoch = relativeDifference.x[relativeDifference.y.indexOf(highestDifference)];
    fillCard('highest-difference-percentage', 'Highest relative difference', `${highestDifference}%`, `Epoch ${highestDifferenceEpoch}`);

    const highestAbsolutDifference = Math.max(...difference.y);
    const highestAbsolutDifferenceEpoch = difference.x[difference.y.indexOf(highestAbsolutDifference)];
    fillCard('highest-absolut-difference', 'Highest absolut difference', `${highestAbsolutDifference}₳`, `Epoch ${highestAbsolutDifferenceEpoch}`);

    const averageDifferenceMedian = relativeDifference.y.sort((a, b) => a - b)[Math.round(relativeDifference.y.length / 2)];
    fillCard('average-difference-percentage', 'Average relative difference', `${averageDifferenceMedian}%`, 'Median');

    const averageAbsolutDifferenceMedian = difference.y.sort((a, b) => a - b)[Math.round(difference.y.length / 2)]
    fillCard('average-absolut-difference', 'Average absolut difference', `${averageAbsolutDifferenceMedian}₳`, 'Median');

    const absolutDifferences = difference.y.map((difference) => Math.abs(difference));

    const exactMatches = absolutDifferences.filter((difference) => difference < 1).length;
    fillCard('exact-matches', 'Exact matches', exactMatches, 'Difference is 0₳');
    fillCard('difference-1-100-ADA', 'Difference between 1₳ and 100₳', `${absolutDifferences.filter((difference) => difference >= 1 && difference < 100).length}`, '');
    fillCard('difference-100-500-ADA', 'Difference between 100₳ and 500₳', `${absolutDifferences.filter((difference) => difference >= 100 && difference < 500).length}`, '');
    fillCard('difference-500-1000-ADA', 'Difference between 500₳ and 1000₳', `${absolutDifferences.filter((difference) => difference >= 500 && difference < 1000).length}`, '');
    fillCard('difference-1k-5k-ADA', 'Difference between 1k₳ and 5k₳', `${absolutDifferences.filter((difference) => difference >= 1000 && difference < 5000).length}`, '');
    fillCard('difference-5k-10k-ADA', 'Difference between 5k₳ and 10k₳', `${absolutDifferences.filter((difference) => difference >= 5000 && difference < 10000).length}`, '');
    fillCard('difference-10k-50k-ADA', 'Difference between 10k₳ and 50k₳', `${absolutDifferences.filter((difference) => difference >= 10000 && difference < 50000).length}`, '');
    fillCard('difference-above-50k-ADA', 'Difference above 50k₳', `${absolutDifferences.filter((difference) => difference >= 50000).length}`, '');
};